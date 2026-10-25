"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const http = require("node:http");
const path = require("node:path");
const { spawn } = require("node:child_process");
const { once } = require("node:events");
const { chromium } = require("playwright");

const root = path.resolve(__dirname, "../../..");
const jar = path.join(root, "target/pricewatch-0.0.1-SNAPSHOT.jar");
const artifacts = path.join(root, "target/browser-checks");
const sample = JSON.parse(fs.readFileSync(path.join(root,
  "src/main/resources/samples/pricewatch-products.json"), "utf8"));
const label = text => ({ en: text, "zh-Hant": text, "zh-Hans": text });
// Synthetic records exercise relevance and exact decimal boundaries, not real prices.
function fixture(code, name, category, price = "3.90") {
  return {
    code, brand: label("Browser test"), name: label(name),
    cat1Name: label("Food / Noodles"), cat2Name: label(category), cat3Name: label(category),
    prices: [{ supermarketCode: "TEST", price }], offers: []
  };
}
const records = [...sample,
  fixture("TEST-NOODLE", "Instant noodle 70g", "Instant noodles"),
  fixture("TEST-DIMSUM", "Rice dumpling", "Frozen food"),
  fixture("TEST-TINY", "Decimal precision test", "Test data", "0.0001"),
  fixture("TEST-LARGE", "Large decimal test", "Test data", "999999999999999.9999")];
let missingRetailer = false;
let child;
let browser;
let source;
let appLog = "";
let passed = 0;
const browserErrors = [];

async function check(name, action) {
  await action();
  passed++;
  console.log(`PASS ${name}`);
}

async function startApp(sourceUrl) {
  const java = process.env.JAVA_HOME
    ? path.join(process.env.JAVA_HOME, "bin", process.platform === "win32" ? "java.exe" : "java")
    : "java";
  child = spawn(java, ["-jar", jar, "--server.port=0", "--server.address=127.0.0.1",
    "--spring.datasource.url=jdbc:h2:mem:browser-checks;DB_CLOSE_DELAY=-1",
    "--spring.jpa.hibernate.ddl-auto=create-drop", `--pricewatch.source-url=${sourceUrl}`],
  { cwd: root, stdio: ["ignore", "pipe", "pipe"], windowsHide: true });
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error("App startup timed out; see app.log")), 45000);
    const onData = chunk => {
      appLog += chunk.toString();
      const port = /Tomcat started on port (\d+)/.exec(appLog)?.[1];
      if (port && appLog.includes("Started PricewatchApplication")) {
        clearTimeout(timeout);
        resolve(`http://127.0.0.1:${port}`);
      }
    };
    child.stdout.on("data", onData);
    child.stderr.on("data", onData);
    child.once("error", error => { clearTimeout(timeout); reject(error); });
    child.once("exit", code => {
      clearTimeout(timeout);
      reject(new Error(`App exited during startup (${code}); see app.log`));
    });
  });
}

async function main() {
  assert.ok(fs.existsSync(jar), "Build the app first: ./mvnw package (Windows: .\\mvnw.cmd package)");
  fs.mkdirSync(artifacts, { recursive: true });
  source = http.createServer((request, response) => {
    const data = structuredClone(records);
    if (missingRetailer) {
      data.find(product => product.code === "P000000124").prices
        .find(price => price.supermarketCode === "JASONS").price = null;
    }
    response.writeHead(200, { "Content-Type": "application/json" });
    response.end(JSON.stringify(data));
  });
  source.listen(0, "127.0.0.1");
  await once(source, "listening");
  const base = await startApp(`http://127.0.0.1:${source.address().port}/prices.json`);
  browser = await chromium.launch({ channel: process.env.BROWSER_CHANNEL, headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  context.on("page", page => {
    page.on("pageerror", error => browserErrors.push(error.message));
    page.on("console", message => {
      if (message.type() === "error") browserErrors.push(message.text());
    });
  });
  const page = await context.newPage();
  const go = async route => {
    const response = await page.goto(base + route);
    assert.equal(response.status(), 200, route);
  };
  const text = locator => locator.innerText();
  const chooseRetailer = (tab, code) => tab.locator(".retailer-choice").filter({
    has: tab.getByRole("radio", { name: `Choose ${code}`, exact: true })
  }).click();
  const describedText = radio => radio.evaluate(input =>
    (input.getAttribute("aria-describedby") || "").split(/\s+/)
      .map(id => document.getElementById(id)?.textContent || "").join(" "));

  await check("empty start and sample import", async () => {
    await go("/");
    assert.match(await text(page.locator(".source-banner")), /No prices loaded/);
    await go("/data");
    await page.getByRole("button", { name: "Load checked sample" }).click();
    assert.match(await text(page.locator(".source-banner")), /Historical sample/);
  });

  await check("store name and lowest-price badge select the retailer", async () => {
    for (const width of [375, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      await go("/products/P000000124");
      const lowestRow = page.locator(".retailer-option").filter({ hasText: "AEON" });
      assert.match(await describedText(lowestRow.getByRole("radio")), /7\.50/);
      await lowestRow.locator(".lowest-tag").click();
      assert.equal(await page.getByRole("radio", { name: "Choose AEON", exact: true }).isChecked(), true,
        `Lowest-price badge did not select AEON at ${width}px`);
      assert.match(await text(page.locator("[data-selected-store]")), /AEON/);
      await page.locator(".retailer-option").filter({ hasText: "PARKNSHOP" }).locator(".retailer-price").click();
      assert.equal(await page.getByRole("radio", { name: "Choose PARKNSHOP", exact: true }).isChecked(), true);
      const otherRow = page.locator(".retailer-option").filter({ hasText: "JASONS" });
      await otherRow.getByText("JASONS", { exact: true }).click();
      assert.equal(await page.getByRole("radio", { name: "Choose JASONS", exact: true }).isChecked(), true);
      assert.match(await text(page.locator("[data-listed-estimate]")), /12\.50/);
      await lowestRow.locator(".retailer-choice").click({ position: { x: 2, y: 2 } });
      assert.equal(await page.getByRole("radio", { name: "Choose AEON", exact: true }).isChecked(), true,
        `Store label padding did not select AEON at ${width}px`);
      await page.getByRole("radio", { name: "Choose AEON", exact: true }).focus();
      await page.keyboard.press("ArrowDown");
      assert.equal(await page.getByRole("radio", { name: "Choose JASONS", exact: true }).isChecked(), true);
      assert.match(await text(page.locator("[data-selected-store]")), /JASONS/);
      assert.equal(await otherRow.locator(".selected-tag").isVisible(), true);
      assert.equal(await lowestRow.locator(".selected-tag").isVisible(), false);
      await otherRow.locator("details summary").click();
      assert.equal(await otherRow.locator("details").getAttribute("open"), "");
      assert.equal(await page.getByRole("radio", { name: "Choose JASONS", exact: true }).isChecked(), true);
      await page.screenshot({ path: path.join(artifacts, `store-choice-${width}.png`), fullPage: true });
    }
  });

  await check("retailer selection, quantity preview, and saved choice", async () => {
    await go("/products/P000000124");
    assert.equal(await page.locator("#add-item-form").evaluate(form => form.checkValidity()), false);
    await chooseRetailer(page, "JASONS");
    await page.locator("#quantity").fill("2");
    assert.match(await text(page.locator("[data-listed-estimate]")), /25\.00/);
    assert.match(await text(page.locator(".selection-estimate")), /offer not included/);
    await page.getByRole("button", { name: "Add to my list" }).click();
    await page.waitForURL(base + "/list");
    await page.reload();
    assert.equal(await page.locator("[name=retailerCode]").inputValue(), "JASONS");
    assert.match(await text(page.locator(".line-total")), /25\.00/);
    await page.locator("[name=retailerCode]").selectOption("PARKNSHOP");
    await page.locator("[name=quantity]").fill("3");
    await page.locator("[name=checked]").check();
    await page.getByRole("button", { name: /^Save changes/ }).click();
    await page.reload();
    assert.equal(await page.locator("[name=retailerCode]").inputValue(), "PARKNSHOP");
    assert.equal(await page.locator("[name=checked]").isChecked(), true);
    assert.match(await text(page.locator(".line-total")), /27\.00/);
  });

  await check("forms work with JavaScript disabled", async () => {
    const plain = await browser.newContext({ javaScriptEnabled: false });
    try {
      const tab = await plain.newPage();
      await tab.goto(base + "/products/P000000229");
      await tab.locator(".retailer-option").filter({ hasText: "WELLCOME" })
        .locator(".lowest-tag").click();
      assert.equal(await tab.getByRole("radio", { name: "Choose WELLCOME", exact: true }).isChecked(), true);
      assert.equal(await tab.locator("#add-item-form").evaluate(form => form.checkValidity()), true);
      await tab.getByRole("button", { name: "Add to my list" }).click();
      // A first flash redirect can include Tomcat's URL session marker.
      await tab.waitForURL(url => url.origin === base && url.pathname.split(";")[0] === "/list");
      assert.match(await tab.locator("main").innerText(), /Your retailer: WELLCOME/);
    } finally { await plain.close(); }
  });

  await check("search relevance, normalization, and category URL state", async () => {
    await go("/data");
    await page.getByRole("button", { name: "Refresh live prices" }).click();
    await go("/?q=NOODLE");
    assert.match(await text(page.locator(".product-grid")), /TEST-NOODLE/);
    assert.doesNotMatch(await text(page.locator(".product-grid")), /TEST-DIMSUM/);
    await page.getByRole("link", { name: /^Related categories/ }).click();
    assert.match(await text(page.locator(".product-grid")), /TEST-DIMSUM/);
    await page.locator("#product-search").fill("milk");
    await page.getByRole("button", { name: "Search", exact: true }).click();
    assert.equal(new URL(page.url()).searchParams.has("related"), false);
    assert.match(await text(page.locator(".product-grid")), /P000000124/);
    await go("/?q=NOODLE&related=true");
    await page.locator("#product-search").fill("");
    await page.getByRole("button", { name: "Search", exact: true }).click();
    assert.equal(await page.locator(".product-card").count(), records.length);
    await go("/?related=true");
    assert.equal(await page.locator(".product-card").count(), records.length);
    await go("/?q=" + encodeURIComponent(" ＮＯＯＤＬＥ "));
    assert.match(await text(page.locator(".product-grid")), /TEST-NOODLE/);
    await page.locator("#category-filter").selectOption("Instant noodles");
    await page.waitForURL(url => url.searchParams.get("category") === "Instant noodles");
    await page.reload();
    assert.equal(await page.locator("#category-filter").inputValue(), "Instant noodles");
    assert.doesNotMatch(await text(page.locator(".product-grid")), /TEST-DIMSUM/);
    for (const query of ["鮮牛奶", "鲜牛奶"]) {
      await go("/?q=" + encodeURIComponent(query));
      assert.match(await text(page.locator(".product-grid")), /P000000124/);
    }
  });

  await check("decimal preview boundaries and invalid quantities", async () => {
    await go("/products/TEST-TINY");
    await chooseRetailer(page, "TEST");
    await page.locator("#quantity").fill("3");
    assert.match(await text(page.locator("[data-listed-estimate]")), /0\.0003$/);
    await page.locator("#quantity").fill("1e1");
    assert.equal(await text(page.locator("[data-listed-estimate]")), "—");
    await go("/products/TEST-LARGE");
    await chooseRetailer(page, "TEST");
    await page.locator("#quantity").fill("999");
    assert.match(await text(page.locator("[data-listed-estimate]")), /998,999,999,999,999,999\.9001$/);
    await page.getByRole("button", { name: "Add to my list" }).click();
    await page.waitForURL(base + "/list");
  });

  await check("missing selected price stays unknown after refresh", async () => {
    await go("/list");
    const item = page.locator(".list-item").filter({ hasText: "P000000124" });
    await item.locator("[name=retailerCode]").selectOption("JASONS");
    await item.getByRole("button", { name: /^Save changes/ }).click();
    missingRetailer = true;
    await go("/data");
    await page.getByRole("button", { name: "Refresh live prices" }).click();
    await go("/products/P000000124");
    const unavailable = page.locator(".retailer-option").filter({ hasText: "JASONS" });
    assert.match(await text(unavailable), /No current price/);
    assert.equal(await unavailable.getByRole("radio").isDisabled(), true);
    assert.match(await describedText(unavailable.getByRole("radio")), /No current price/);
    const unavailableBounds = await unavailable.locator(".retailer-choice").boundingBox();
    await page.mouse.click(unavailableBounds.x + 20, unavailableBounds.y + 20);
    assert.equal(await unavailable.getByRole("radio").isChecked(), false);
    await go("/list");
    assert.equal(await item.locator("[name=retailerCode]").inputValue(), "JASONS");
    assert.match(await text(item.locator(".line-total")), /Not listed/);
    assert.match(await text(page.locator(".basket-preview")), /Total is incomplete/);
  });

  await check("navigation, responsive layouts, and keyboard skip link", async () => {
    for (const width of [375, 768, 1024, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      for (const route of ["/", "/products/P000000124", "/products/TEST-LARGE", "/list", "/data"]) {
        await go(route);
        assert.equal(await page.locator("h1").count(), 1);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true,
          `Horizontal overflow: ${width} ${route}`);
        assert.equal(await page.locator("#main-content").count(), 1);
        if (route === "/") {
          assert.equal(await page.locator(".product-price").evaluateAll(prices => prices.every(price => {
            const details = price.closest(".product-details").getBoundingClientRect();
            const bounds = price.getBoundingClientRect();
            return bounds.right <= details.right && price.scrollWidth <= price.clientWidth;
          })), true, `Clipped card price at ${width}px`);
        }
        if (route === "/list") {
          assert.equal(await page.locator(".line-total, .summary-total strong").evaluateAll(amounts =>
            amounts.every(amount => amount.scrollWidth <= amount.clientWidth &&
              amount.getBoundingClientRect().right <= amount.parentElement.getBoundingClientRect().right)),
          true, `Clipped saved total at ${width}px`);
        }
        if (route === "/products/TEST-LARGE") {
          await chooseRetailer(page, "TEST");
          await page.locator("#quantity").fill("999");
          assert.equal(await page.locator("[data-listed-estimate]").evaluate(amount =>
            amount.scrollWidth <= amount.clientWidth), true, `Clipped estimate at ${width}px`);
        }
      }
    }
    await page.setViewportSize({ width: 375, height: 1000 });
    await go("/");
    await page.locator(".mobile-nav").getByRole("link", { name: "Price data", exact: true }).click();
    await page.waitForURL(base + "/data");
    await page.locator(".mobile-more > summary").click();
    await page.locator(".mobile-nav").getByRole("link", { name: /Saved/ }).click();
    await page.waitForURL(base + "/planned/saved");
    assert.match(await text(page.locator("main")), /PLANNED · NOT AVAILABLE YET/);
    await go("/");
    await page.keyboard.press("Tab");
    assert.equal(await page.locator(".skip-link").evaluate(link => link === document.activeElement), true);
    await page.keyboard.press("Enter");
    assert.equal(await page.locator("#main-content").evaluate(main => main === document.activeElement), true);
    await page.screenshot({ path: path.join(artifacts, "mobile.png"), fullPage: true, animations: "disabled" });
    await page.setViewportSize({ width: 1440, height: 1000 });
    await go("/list");
    await page.screenshot({ path: path.join(artifacts, "list.png"), fullPage: true, animations: "disabled" });
  });

  await check("no browser script or console errors", async () => assert.deepEqual(browserErrors, []));
  console.log(`${passed} browser scenarios passed. Artifacts: ${path.relative(root, artifacts)}`);
}

main().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  try {
    if (browser) await browser.close();
  } finally {
    if (child?.pid && child.exitCode === null && child.signalCode === null) {
      const closed = once(child, "exit");
      child.kill();
      await closed;
    }
    if (source) await new Promise(resolve => source.close(resolve));
    if (fs.existsSync(artifacts)) fs.writeFileSync(path.join(artifacts, "app.log"), appLog);
  }
});


