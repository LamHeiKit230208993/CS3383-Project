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
const children = [];
let browser;
let source;
let sourceReads = 0;
let appLog = "";
let passed = 0;
const browserErrors = [];

async function check(name, action) {
  await action();
  passed++;
  console.log(`PASS ${name}`);
}

async function tabTo(page, target) {
  for (let attempt = 0; attempt < 40; attempt++) {
    if (await target.evaluate(element => element === document.activeElement)) return;
    await page.keyboard.press("Tab");
  }
  assert.fail("Keyboard focus did not reach the expected control.");
}

async function startApp(sourceUrl) {
  const java = process.env.JAVA_HOME
    ? path.join(process.env.JAVA_HOME, "bin", process.platform === "win32" ? "java.exe" : "java")
    : "java";
  const child = spawn(java, ["-jar", jar, "--server.port=0", "--server.address=127.0.0.1",
    "--spring.datasource.url=jdbc:h2:mem:browser-checks;DB_CLOSE_DELAY=-1",
    "--spring.jpa.hibernate.ddl-auto=create-drop", "--pricewatch.jobs.enabled=false",
    `--pricewatch.source-url=${sourceUrl}`],
  { cwd: root, env: { ...process.env, DEEPSEEK_API_KEY_SKU: "" },
    stdio: ["ignore", "pipe", "pipe"], windowsHide: true });
  children.push(child);
  let output = "";
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error("App startup timed out; see app.log")), 45000);
    const onData = chunk => {
      appLog += chunk.toString();
      output += chunk.toString();
      const port = /Tomcat started on port (\d+)/.exec(output)?.[1];
      if (port && output.includes("Started PricewatchApplication")) {
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
    sourceReads++;
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
  let exportedList;
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

  await check("Ask saves messages ahead of replies, recovers interrupted turns, and reopens chats on mobile", async () => {
    const freshSession = await browser.newContext();
    try {
      const freshAsk = await freshSession.newPage();
      await freshAsk.goto(base + "/ask");
      const input = freshAsk.getByRole("textbox", { name: "Your price question" });
      await input.fill("who are u");
      await input.press("Enter");
      await freshAsk.locator("[data-ask-status]").filter({ hasText: "Reply received." }).waitFor({ state: "attached" });
      assert.match(await freshAsk.locator(".chat-message").last().innerText(), /Market Desk, your shopping assistant/);
    } finally {
      await freshSession.close();
    }
    await go("/ask");
    const askStatus = page.locator("[data-ask-status]");
    const questionField = page.getByRole("textbox", { name: "Your price question" });
    const transcript = page.locator("[data-ask-thread]");
    const sendButton = page.getByRole("button", { name: "Send", exact: true });
    const sendEndpoint = /\/ask\/send(?:\?.*)?$/;
    const replyEndpoint = /\/ask\/reply(?:\?.*)?$/;
    const send = async question => {
      await questionField.fill(question);
      await sendButton.click();
      await askStatus.filter({ hasText: "Reply received." }).waitFor({ state: "attached" });
    };
    let releaseReply;
    const replyGate = new Promise(resolve => { releaseReply = resolve; });
    const holdReply = async route => {
      await replyGate;
      await route.continue();
    };
    await page.route(replyEndpoint, holdReply);
    try {
      await questionField.fill("Kowloon Dairy fresh milk 236mL");
      await questionField.press("Enter");
      await page.locator("[data-ask-working]").waitFor();
      assert.equal(await sendButton.isDisabled(), true);
      assert.match(await text(page.locator(".chat-shopper")), /Kowloon Dairy fresh milk 236mL/);
      assert.equal(await page.locator(".chat-pending").count(), 0);
      assert.equal(await transcript.getAttribute("data-awaiting-reply"), "true");
      assert.match(page.url(), /\/ask\?chatId=/);
      const accepted = await page.request.get(page.url());
      assert.match(await accepted.text(), /Kowloon Dairy fresh milk 236mL/);
      await questionField.fill("What about Wellcome?");
      await page.screenshot({ path: path.join(artifacts, "ask-pending.png") });
    } finally {
      releaseReply();
    }
    await askStatus.filter({ hasText: "Reply received." }).waitFor({ state: "attached" });
    await page.unroute(replyEndpoint, holdReply);
    assert.equal(await questionField.inputValue(), "What about Wellcome?");
    const firstChat = page.url();
    assert.match(firstChat, /\/ask\?chatId=/);
    const failSend = route => route.request().method() === "POST"
      ? route.fulfill({ status: 200, contentType: "text/plain", body: "No reply was returned" })
      : route.continue();
    await page.route(sendEndpoint, failSend);
    await questionField.fill("What about Wellcome?");
    await sendButton.click();
    await askStatus.filter({ hasText: "Try again" }).waitFor();
    assert.equal(await questionField.inputValue(), "What about Wellcome?");
    assert.equal(await page.locator(".chat-message").count(), 2);
    assert.equal(await sendButton.isEnabled(), true);
    await page.unroute(sendEndpoint, failSend);
    await page.route(replyEndpoint, failSend);
    await sendButton.click();
    const retryReply = page.getByRole("button", { name: "Try reply again" });
    await retryReply.waitFor();
    assert.equal(await page.locator(".chat-shopper").count(), 2);
    assert.match(await text(page.locator(".chat-message").last()), /What about Wellcome/);
    await page.unroute(replyEndpoint, failSend);
    await retryReply.click();
    await askStatus.filter({ hasText: "Reply received." }).waitFor({ state: "attached" });
    assert.match(await text(page.locator(".chat-message").last()), /236mL at WELLCOME · HK\$12\.00/);
    const alignments = await page.locator(".chat-message").evaluateAll(messages =>
      messages.map(message => getComputedStyle(message).justifySelf));
    assert.deepEqual(alignments, ["end", "start", "end", "start"]);
    assert.equal(await page.locator(".chat-messages").evaluate(list => getComputedStyle(list).listStyleType), "none");
    await page.getByRole("link", { name: /New chat/ }).click();
    assert.equal(await page.locator(".chat-message").count(), 0);
    await send("no-such-product");
    assert.notEqual(page.url(), firstChat);
    assert.match(await text(page.locator(".chat-message").last()), /No matching product/);
    await send("who are u");
    assert.match(await text(page.locator(".chat-message").last()), /Market Desk, your shopping assistant/);
    await page.setViewportSize({ width: 375, height: 667 });
    await page.locator(".ask-recent summary").click();
    await page.getByRole("link", { name: "Kowloon Dairy fresh milk 236mL" }).click();
    assert.equal(page.url(), firstChat);
    assert.equal(await page.locator(".chat-message").count(), 4);
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
    const sendButtonBounds = await sendButton.boundingBox();
    const mobileNav = await page.locator(".mobile-nav").boundingBox();
    assert.ok(sendButtonBounds && mobileNav && sendButtonBounds.y + sendButtonBounds.height <= mobileNav.y,
      "Mobile navigation must not cover the composer");
    const visibleMessageCount = await transcript.evaluate(region => {
      const viewport = region.getBoundingClientRect();
      return [...region.querySelectorAll(".chat-message")].filter(message => {
        const rect = message.getBoundingClientRect();
        const visible = Math.min(viewport.bottom, rect.bottom) - Math.max(viewport.top, rect.top);
        return visible >= rect.height / 2;
      }).length;
    });
    assert.ok(visibleMessageCount >= 3, "At least three messages should be readable on a short mobile viewport");
    await page.screenshot({ path: path.join(artifacts, "ask-mobile.png"), fullPage: true });
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.screenshot({ path: path.join(artifacts, "ask-desktop.png"), fullPage: true });
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
    assert.equal(await text(item.locator(".line-total")), "Unknown");
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
    await page.waitForURL(base + "/saved");
    assert.match(await text(page.locator("main")), /Wishlist/);
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

  await check("J10 exports one list and imports an editable copy on a second local app", async () => {
    await go("/list");
    const precisionItem = page.locator(".list-item").filter({ hasText: "TEST-LARGE" });
    await precisionItem.locator("summary").click();
    await precisionItem.getByRole("button", { name: /^Confirm remove/ }).click();
    assert.equal(await page.locator(".list-item").count(), 2);
    const milk = page.locator(".list-item").filter({ hasText: "P000000124" });
    await milk.locator("select[name=retailerCode]").selectOption("AEON");
    await milk.getByRole("checkbox", { name: "Checked" }).check();
    await milk.getByRole("button", { name: /Save changes/ }).click();
    const [download] = await Promise.all([
      page.waitForEvent("download"),
      page.getByRole("link", { name: "Export JSON" }).click()
    ]);
    const file = await download.path();
    exportedList = fs.readFileSync(file);
    const exported = JSON.parse(fs.readFileSync(file, "utf8"));
    assert.equal(exported.version, 1);
    assert.equal(exported.items.length, 2);
    const original = exported.items.find(item => item.productCode === "P000000124");
    assert.equal(original.checked, true);
    assert.equal(original.retailerCode, "AEON");
    const receivingBase = await startApp(`http://127.0.0.1:${source.address().port}/prices.json`);
    const receivingContext = await browser.newContext();
    try {
      const receiver = await receivingContext.newPage();
      await receiver.goto(receivingBase + "/list");
      const upload = async buffer => {
        await receiver.locator("#list-file").setInputFiles({
          name: "shopping-list.json", mimeType: "application/json", buffer
        });
        await receiver.getByRole("button", { name: "Import JSON" }).click();
      };
      await upload(exportedList);
      assert.match(new URL(receiver.url()).searchParams.get("listId") ?? "", /^\d+$/,
        await receiver.locator("main").innerText());
      const copy = receiver.locator(".list-item").filter({ hasText: "P000000124" });
      assert.equal(await receiver.locator(".list-item").count(), 2);
      assert.equal(await copy.locator(".line-total").innerText(), "Unknown");
      assert.equal(await copy.locator("select[name=retailerCode]").inputValue(), "AEON");
      assert.equal(await copy.getByRole("checkbox", { name: "Checked" }).isChecked(), true);
      await copy.getByRole("spinbutton").fill(String(original.quantity + 1));
      await copy.getByRole("button", { name: /Save changes/ }).click();
      assert.equal(await copy.getByRole("spinbutton").inputValue(), String(original.quantity + 1));
      assert.equal(await milk.getByRole("spinbutton").inputValue(), String(original.quantity));
      await upload(Buffer.from('{"version":1,"items":['));
      assert.match(await receiver.getByRole("alert").innerText(), /Could not import/);
      assert.equal(await receiver.locator("#list-picker option").count(), 2);
      await receiver.locator("#list-picker").selectOption({ index: 1 });
      await receiver.getByRole("button", { name: "Open list" }).click();
      assert.equal(await copy.getByRole("spinbutton").inputValue(), String(original.quantity + 1));
      await receiver.screenshot({ path: path.join(artifacts, "j10-imported-list.png"), fullPage: true });
    } finally {
      await receivingContext.close();
    }
  });
  await check("J11 reopens the imported list offline and guards writes and remote actions", async () => {
    await go("/list");
    await page.locator("#list-file").setInputFiles({
      name: "shopping-list.json", mimeType: "application/json", buffer: exportedList
    });
    await page.getByRole("button", { name: "Import JSON" }).click();
    const listId = new URL(page.url()).searchParams.get("listId");
    assert.ok(listId);
    const rows = page.locator(".list-item");
    const quantities = await rows.locator("input[name=quantity]").evaluateAll(inputs => inputs.map(input => input.value));
    const amounts = await rows.locator(".line-total").allTextContents();
    const cachedRetrieved = (await page.locator(".source-banner span").first().innerText())
      .match(/Retrieved (.+) ·/)[1];
    const readsBefore = sourceReads;
    const localhostOnly = route => new URL(route.request().url()).origin === base
      ? route.continue() : route.abort("internetdisconnected");
    await context.route("**/*", localhostOnly);
    try {
      await page.getByRole("button", { name: "View offline" }).click();
      await page.waitForURL(url => url.pathname.split(";")[0] === "/offline");
      await page.reload();
      assert.match(await page.locator("main").innerText(), /READ ONLY/);
      assert.equal(await page.locator("main time").innerText(), cachedRetrieved);
      assert.deepEqual(await page.locator(".offline-item dd:first-of-type").evaluateAll(fields =>
        fields.filter(field => field.previousElementSibling.textContent === "Packs")
          .map(field => field.textContent)), quantities);
      assert.deepEqual(await page.locator(".offline-item .line-total").allTextContents(), amounts);
      assert.match(await page.locator(".offline-item").filter({ hasText: "P000000124" }).innerText(), /Checked[\s\S]*AEON/);
      assert.equal(await page.getByRole("button", { name: /Save changes|Refresh live prices|Send/ }).count(), 0);
      for (const route of ["/list/items/update", "/data/refresh", "/ask"]) {
        assert.equal((await page.request.post(base + route)).status(), 403, route);
      }
      await page.goto(base + "/ask");
      assert.equal(new URL(page.url()).pathname, "/offline");
      assert.equal(sourceReads, readsBefore);
      await page.screenshot({ path: path.join(artifacts, "j11-offline-list.png"), fullPage: true });
      await page.getByRole("button", { name: "Exit offline review" }).click();
      assert.equal(new URL(page.url()).searchParams.get("listId"), listId);
      assert.equal(await page.getByRole("button", { name: /Save changes/ }).count(), 2);
      await go("/ask");
      assert.equal(await page.getByRole("button", { name: "Send", exact: true }).count(), 1);
    } finally {
      await context.unroute("**/*", localhostOnly);
    }
  });
  await check("J12 finds the same pack in three languages, with usable keyboard focus and high contrast", async () => {
    const milk = page.locator(".product-card").filter({ hasText: "P000000124" });
    const search = page.locator("#product-search");
    for (const [language, query, sourceLabel, width] of [
      ["en", "Fresh Milk", "100 %Fresh Milk 236mL", 1440],
      ["zh-Hant", "鮮牛奶", "鮮牛奶 236毫升", 1440],
      ["zh-Hans", "鲜牛奶", "鲜牛奶 236毫升", 375]
    ]) {
      await page.setViewportSize({ width, height: 900 });
      await go("/preferences");
      const languageField = page.locator("select[name=language]");
      const contrast = page.locator("input[name=highContrast]");
      await tabTo(page, languageField);
      await page.keyboard.press("Home");
      for (let index = 0; index < ["en", "zh-Hant", "zh-Hans"].indexOf(language); index++) {
        await page.keyboard.press("ArrowDown");
      }
      assert.equal(await languageField.inputValue(), language);
      await tabTo(page, contrast);
      if (!await contrast.isChecked()) await page.keyboard.press("Space");
      assert.equal(await contrast.isChecked(), true);
      const outline = await contrast.evaluate(element => getComputedStyle(element).outlineStyle);
      assert.equal(outline, "solid");
      await tabTo(page, page.locator('form[action$="/preferences"] button[type=submit]'));
      await page.keyboard.press("Enter");
      await page.locator(".preferences-panel[role=status]").waitFor();
      await go("/");
      assert.equal(await page.locator("html").getAttribute("lang"), language);
      assert.match(await page.locator("html").getAttribute("class"), /\bhigh-contrast\b/);
      await tabTo(page, search);
      await page.keyboard.insertText(query);
      await page.keyboard.press("Enter");
      await page.waitForURL(url => url.searchParams.get("q") === query);
      assert.ok((await milk.innerText()).includes(sourceLabel), await milk.innerText());
      assert.equal(await page.locator("body").evaluate(body => body.scrollWidth <= body.clientWidth), true);
      await go("/ask");
      await page.locator("#price-question").fill("P000000124");
      await page.locator("#price-question").press("Enter");
      await page.locator(".chat-message:not(.chat-shopper)").waitFor();
      const reply = await page.locator(".chat-message").last().innerText();
      assert.ok(reply.includes(sourceLabel), reply);
      assert.match(reply, /HK\$7\.50/);
      if (language !== "en") assert.doesNotMatch(reply, /Lowest listed|AI explanation is unavailable/);
      await page.screenshot({ path: path.join(artifacts, `j12-ask-${language}.png`), fullPage: true });
      await go("/products/P000000124");
      const retailerRadio = page.locator('input[name=retailerCode][value=AEON]');
      const retailerCard = page.locator(".retailer-option").filter({ has: retailerRadio });
      await tabTo(page, retailerRadio);
      assert.equal(await retailerCard.evaluate(card => getComputedStyle(card).outlineStyle), "solid");
      await page.keyboard.press("Space");
      assert.equal(await retailerRadio.isChecked(), true);
      assert.equal(await retailerCard.locator(".selected-tag").isVisible(), true);
      assert.equal(await page.locator("[data-selected-store]").innerText(),
        language === "en" ? "AEON · 1 packs" : "AEON · 1 件");
      if (width === 375) {
        await page.screenshot({ path: path.join(artifacts, `j12-compare-${language}.png`), fullPage: true });
      }
      if (language !== "en") {
        for (const route of ["/products/P000000124", "/list", "/saved", "/alerts", "/budget", "/reminders", "/data"]) {
          await go(route);
          assert.doesNotMatch(await page.locator("main").innerText(), /\?\?[a-z][\w.]*[^\s]*\?\?/);
          assert.match(await page.locator("main h1").innerText(), /\p{Script=Han}/u);
          assert.equal(await page.locator("body").evaluate(body => body.scrollWidth <= body.clientWidth), true, route);
        }
      }
    }
    await go("/preferences");
    assert.equal(await page.locator("input[name=highContrast]").isChecked(), true);
    await page.locator("select[name=language]").selectOption("en");
    await page.locator("input[name=highContrast]").uncheck();
    await page.locator('form[action$="/preferences"] button[type=submit]').click();
    await page.setViewportSize({ width: 1440, height: 1000 });
  });
  await check("J12 shows retained prices, a recorded gap, and an untracked pack's empty state", async () => {
    const milkCode = "P000000124";
    const history = `/products/${milkCode}/history`;
    const otherCode = sample.find(product => product.code !== milkCode).code;
    await go(`/products/${otherCode}/history`);
    assert.match(await page.locator("main").innerText(), /No saved price observations/);
    missingRetailer = false;
    await go("/data");
    await page.getByRole("button", { name: "Refresh live prices", exact: true }).click();
    await go(`/saved?code=${milkCode}`);
    await page.getByRole("button", { name: "Save to wishlist" }).click();
    await go(`/saved?code=${milkCode}`);
    const trackAll = page.locator('form[action$="/tracking/start-all"]').first();
    await trackAll.getByRole("button", { name: "Track all retailers" }).click();
    await page.getByRole("button", { name: "Stop all retailers" }).waitFor();
    missingRetailer = true;
    await go("/data");
    await page.getByRole("button", { name: "Refresh live prices", exact: true }).click();
    await go(`/products/${milkCode}`);
    await page.getByRole("link", { name: "Price history", exact: true }).click();
    const rows = page.locator(".price-history tbody tr").filter({ hasText: "JASONS" });
    assert.equal(await rows.count(), 2);
    assert.match(await rows.first().innerText(), /Unknown/);
    assert.match(await rows.last().innerText(), /HK\$12\.50/);
    const dates = await rows.locator("time").evaluateAll(items => items.map(item => Date.parse(item.dateTime)));
    assert.ok(Number.isFinite(dates[0]) && dates[0] >= dates[1]);
    await go("/preferences");
    await page.locator("select[name=language]").selectOption("zh-Hant");
    await page.locator("input[name=highContrast]").check();
    await page.locator('form[action$="/preferences"] button[type=submit]').click();
    await page.setViewportSize({ width: 375, height: 900 });
    await go(history);
    assert.match(await page.locator("main h1").innerText(), /價格記錄/);
    assert.match(await page.locator("main .intro").innerText(), /鮮牛奶 236毫升/);
    assert.doesNotMatch(await rows.first().innerText(), /Unknown/);
    assert.equal(await page.locator("body").evaluate(body => body.scrollWidth <= body.clientWidth), true);
    assert.equal(await rows.last().locator("td").last().evaluate(cell => {
      const amount = document.createRange();
      amount.selectNodeContents(cell);
      return amount.getClientRects().length;
    }), 1, "The listed price fits on a single line at 375px.");
    await page.screenshot({ path: path.join(artifacts, "j12-history-mobile.png"), fullPage: true });
    await go("/preferences");
    await page.locator("select[name=language]").selectOption("en");
    await page.locator("input[name=highContrast]").uncheck();
    await page.locator('form[action$="/preferences"] button[type=submit]').click();
    await page.setViewportSize({ width: 1440, height: 1000 });
  });
  await check("J12 exports personal records, cancels reset, then clears them and keeps the catalogue", async () => {
    for (const [route, form] of [
      ["/budget", { period: "MONTHLY", limit: "250" }],
      ["/reminders/create", { code: "P000000124", quantity: "1", intervalDays: "14", retailerCode: "AEON" }],
      ["/alerts/settings", { quietStart: "22:00", quietEnd: "07:00", summaryTime: "08:00" }],
      ["/preferences", { language: "en", highContrast: "true" }]
    ]) {
      assert.equal((await page.request.post(base + route, { form, maxRedirects: 0 })).status(), 302, route);
    }
    await go("/alerts");
    const target = page.locator("article.list-item").filter({ hasText: /AEON · P000000124/ });
    await target.locator("[name=targetPrice]").fill("8");
    await target.getByRole("button", { name: "Save target" }).click();
    await page.locator(".main-nav").getByRole("link", { name: "Personal data", exact: true }).click();
    const exportData = async () => {
      const download = page.waitForEvent("download");
      await page.getByRole("link", { name: "Export personal data" }).click();
      return JSON.parse(fs.readFileSync(await (await download).path(), "utf8"));
    };
    const before = await exportData();
    assert.equal(before.version, 1);
    for (const key of ["lists", "wishlist", "tracking", "observations", "alerts", "reminders", "budgets", "chats"]) {
      assert.ok(before[key].length > 0, key);
    }
    assert.ok(before.lists.some(list => list.entries.some(entry => entry.productCode === "P000000124")));
    assert.ok(before.allRetailerTrackingProducts.includes("P000000124"));
    assert.equal(before.preferences.display.highContrast, true);
    assert.ok(before.preferences.notifications);
    await page.getByRole("link", { name: "Review reset" }).click();
    await page.getByRole("link", { name: "Cancel", exact: true }).click();
    assert.deepEqual(await exportData(), before);
    await page.screenshot({ path: path.join(artifacts, "j12-personal-data.png"), fullPage: true });
    await page.getByRole("link", { name: "Review reset" }).click();
    await page.getByRole("checkbox", { name: /understand that this deletes/ }).check();
    await page.getByRole("button", { name: "Confirm reset" }).click();
    assert.match(await page.locator("main [role=status]").innerText(), /Personal data was reset/);
    const after = await exportData();
    for (const key of ["lists", "wishlist", "allRetailerTrackingProducts", "tracking", "observations", "alerts", "reminders", "budgets", "chats"]) {
      assert.deepEqual(after[key], [], key);
    }
    assert.equal(after.preferences.display.highContrast, false);
    assert.equal(after.preferences.notifications, null);
    await go("/products/P000000124");
    assert.match(await page.locator(".product-identity").innerText(), /100 %Fresh Milk 236mL/);
    assert.match(await page.locator(".source-banner").innerText(), /Saved source prices/);
    await go("/products/P000000124/history");
    assert.match(await page.locator("main").innerText(), /No saved price observations/);
  });
  await check("no browser script or console errors", async () => assert.deepEqual(browserErrors, []));
  console.log(`${passed} browser scenarios passed. Artifacts: ${path.relative(root, artifacts)}`);
}

main().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  try {
    if (browser) await browser.close();
  } finally {
    await Promise.all(children.filter(child => child.pid && child.exitCode === null && child.signalCode === null)
      .map(async child => {
      const closed = once(child, "exit");
      child.kill();
      await closed;
      }));
    if (source) await new Promise(resolve => source.close(resolve));
    if (fs.existsSync(artifacts)) fs.writeFileSync(path.join(artifacts, "app.log"), appLog);
  }
});


