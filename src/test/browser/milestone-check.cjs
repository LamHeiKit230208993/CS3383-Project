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
const artifacts = path.join(root, "target/browser-milestone");
const label = value => ({ en: value, "zh-Hant": value, "zh-Hans": value });
let milkPrice = "10.00";
let secondMilkPrice = "11.00";
let child;
let browser;
let source;
let appLog = "";
let passed = 0;
const browserErrors = [];

function products() {
  return [
    {
      code: "E2E-MILK", brand: label("Browser Test"), name: label("Fresh Milk 236mL"),
      cat1Name: label("Food / Drinks"), cat2Name: label("Milk"), cat3Name: label("Fresh milk"),
      prices: [{ supermarketCode: "TEST-STORE", price: milkPrice },
        { supermarketCode: "OTHER-STORE", price: secondMilkPrice }], offers: []
    },
    {
      code: "E2E-NOODLE", brand: label("Browser Test"), name: label("Instant Noodles 70g"),
      cat1Name: label("Food / Noodles"), cat2Name: label("Noodles"), cat3Name: label("Instant noodles"),
      prices: [{ supermarketCode: "NOODLE-STORE", price: "5.00" }], offers: []
    }
  ];
}

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
    "--spring.datasource.url=jdbc:h2:mem:browser-milestone;DB_CLOSE_DELAY=-1",
    "--spring.jpa.hibernate.ddl-auto=create-drop", "--pricewatch.jobs.enabled=false",
    `--pricewatch.source-url=${sourceUrl}`],
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
  assert.ok(fs.existsSync(jar), "Package the app first: .\\mvnw.cmd package");
  fs.mkdirSync(artifacts, { recursive: true });
  source = http.createServer((_request, response) => {
    response.writeHead(200, { "Content-Type": "application/json" });
    response.end(JSON.stringify(products()));
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
  await page.clock.install({ time: new Date("2026-11-06T12:00:00Z") });
  const go = async route => {
    const response = await page.goto(base + route);
    assert.equal(response.status(), 200, route);
  };
  const submit = async (button, pathname) => {
    await Promise.all([
      page.waitForNavigation({ waitUntil: "domcontentloaded" }),
      button.click()
    ]);
    // Spring can rewrite the first redirect before the session cookie is established.
    assert.equal(new URL(page.url()).pathname.split(";")[0], pathname);
  };
  const capture = async name => page.screenshot({
    path: path.join(artifacts, `${name}.png`), fullPage: true, animations: "disabled"
  });

  await check("accept the two-product local feed", async () => {
    await go("/data");
    await submit(page.getByRole("button", { name: "Refresh live prices" }), "/data");
    assert.match(await page.locator(".source-banner").innerText(), /Saved source prices/);
  });

  await check("wishlist save starts price tracking and exposes row actions", async () => {
    await go("/saved?code=E2E-MILK");
    await submit(page.getByRole("button", { name: "Save to wishlist" }), "/saved");
    await page.locator(".wishlist-row summary").click();
    await page.locator(".wishlist-actions").getByRole("link", { name: "Price history" }).click();
    assert.match(await page.locator("main").innerText(), /TEST-STORE/);
  });

  await check("one product target groups retailers and keeps legacy drop evidence", async () => {
    const exportData = await (await page.request.get(base + "/personal-data/export")).json();
    const trackingId = String(exportData.tracking.find(period => period.retailerCode === "TEST-STORE").id);
    const seeded = await page.request.post(base + "/alerts/drop", { form: { trackingId, enabled: "true" } });
    assert.equal(seeded.status(), 200);
    await go("/saved?code=E2E-MILK");
    await page.getByRole("link", { name: "Set rules", exact: true }).click();
    await page.waitForURL(url => url.pathname === "/alerts/rules");
    await page.locator('[action="/alerts/product-target"] [name="targetPrice"]').fill("9.00");
    await submit(page.locator('[action="/alerts/product-target"] button'), "/alerts/rules");
    assert.match(await page.locator("main").innerText(), /Retailer-specific rules/);
    milkPrice = "8.00";
    secondMilkPrice = "8.50";
    await go("/data");
    await submit(page.getByRole("button", { name: "Refresh live prices" }), "/data");
    await go("/alerts");
    const grouped = page.locator(".alert-row").filter({ has: page.locator(".alert-retailers") });
    assert.equal(await grouped.count(), 1);
    assert.equal(await grouped.locator(".alert-retailers div").count(), 2);
    assert.match(await grouped.innerText(), /OTHER-STORE\s+HK\$8\.50/);
    assert.match(await grouped.innerText(), /TEST-STORE\s+HK\$8\.00/);
    assert.match(await page.locator("main").innerText(), /PRICE DROP/);
    assert.match(await page.locator("main").innerText(), /Previous: HK\$10\.00/);
    assert.match(await page.locator("main").innerText(), /Current: HK\$8\.00/);
    await capture("alerts");
  });

  await check("time reminders can be created, edited, and deleted in item rules", async () => {
    await go("/products/E2E-NOODLE");
    await page.getByRole("link", { name: "Set rules" }).click();
    await page.waitForURL(url => url.pathname === "/alerts/rules");
    await page.getByText("Add time reminder", { exact: true }).click();
    const create = page.locator('form[action="/reminders/create"]');
    await create.locator('[name="quantity"]').fill("2");
    await create.locator('[name="retailerCode"]').selectOption("NOODLE-STORE");
    await submit(create.getByRole("button", { name: "Save reminder" }), "/alerts/rules");
    await page.getByText("2 packs · every 7 days", { exact: true }).click();
    const edit = page.locator('form[action$="/update"]');
    await edit.locator('[name="intervalDays"]').fill("14");
    await submit(edit.getByRole("button", { name: "Save reminder" }), "/alerts/rules");
    await page.getByText("2 packs · every 14 days", { exact: true }).click();
    await capture("reminders");
    await submit(page.getByRole("button", { name: "Delete reminder" }), "/alerts/rules");
    assert.equal(await page.locator('form[action$="/update"]').count(), 0);
    await go("/products/E2E-NOODLE");
    await page.locator(".retailer-option").first().click();
    await page.locator("#quantity").fill("2");
    await submit(page.getByRole("button", { name: "Add to Cart" }), "/list");
  });

  await check("budget reflects the known current list estimate", async () => {
    await go("/products/E2E-MILK");
    await page.locator(".retailer-choice").filter({ hasText: "TEST-STORE" }).click();
    await page.locator("#quantity").fill("1");
    await submit(page.getByRole("button", { name: "Add to Cart" }), "/list");
    await go("/budget");
    await page.locator('[name="period"]').selectOption("MONTHLY");
    await page.locator('[name="limit"]').fill("15.00");
    await submit(page.getByRole("button", { name: "Save budget" }), "/personal-data");
    assert.match(await page.locator("main").innerText(), /known subtotal is already above your limit/i);
    assert.match(await page.locator(".summary-total").innerText(), /HK\$18\.00/);
    await capture("budget");
  });

  await check("a later tracked price updates the existing list and budget without repeating the target", async () => {
    milkPrice = "7.00";
    await go("/data");
    await submit(page.getByRole("button", { name: "Refresh live prices" }), "/data");
    await go("/budget");
    assert.match(await page.locator(".summary-total").innerText(), /HK\$17\.00/);
    await go("/list");
    const milk = page.locator("article.list-item").filter({ hasText: "E2E-MILK" });
    assert.equal(await milk.locator('[name="retailerCode"]').inputValue(), "TEST-STORE");
    assert.match(await milk.locator(".line-total").innerText(), /HK\$7\.00/);
    await go("/alerts");
    const events = page.locator('section[aria-labelledby="events-title"] article');
    assert.equal(await events.filter({ hasText: "TARGET PRICE" }).count(), 1);
    assert.equal(await events.filter({ hasText: "PRICE DROP" }).count(), 2);
    milkPrice = "10.00";
    secondMilkPrice = "11.00";
    await go("/data");
    await submit(page.getByRole("button", { name: "Refresh live prices" }), "/data");
    milkPrice = "8.00";
    await submit(page.getByRole("button", { name: "Refresh live prices" }), "/data");
    await submit(page.getByRole("button", { name: "Refresh live prices" }), "/data");
    await go("/alerts");
    assert.equal(await page.locator(".alert-retailers").count(), 2);
  });

  await check("invalid form parameters return client errors or validation redirects, never 500", async () => {
    const invalidReminder = await context.request.post(`${base}/reminders/create`, {
      form: { code: "E2E-NOODLE", quantity: "0", intervalDays: "7" }, maxRedirects: 0
    });
    assert.equal(invalidReminder.status(), 302);
    await go("/alerts/rules?code=E2E-MILK");
    const trackingId = await page.locator('[action="/alerts/target"] [name="trackingId"]').inputValue();
    const invalidTarget = await context.request.post(`${base}/alerts/target`, {
      form: { trackingId, targetPrice: "not-a-price" }, maxRedirects: 0
    });
    assert.equal(invalidTarget.status(), 400);
    const invalidBudget = await context.request.post(`${base}/budget`, {
      form: { period: "MONTHLY", limit: "not-a-number" }, maxRedirects: 0
    });
    assert.equal(invalidBudget.status(), 200);
  });

  await check("clean active pages refresh, but an edited reminder form is left alone", async () => {
    await go("/alerts");
    assert.equal(await page.locator('[data-timed-page="true"]').count(), 1);
    const cleanReload = page.waitForEvent("framenavigated", {
      predicate: frame => frame === page.mainFrame() && new URL(frame.url()).pathname === "/alerts"
    });
    await page.clock.fastForward(60_000);
    await cleanReload;

    await go("/reminders?code=E2E-NOODLE");
    await page.getByText("Add time reminder", { exact: true }).click();
    let navigations = 0;
    const countNavigation = frame => { if (frame === page.mainFrame()) navigations++; };
    page.on("framenavigated", countNavigation);
    const reminderQuantity = page.locator('form[action="/reminders/create"] [name="quantity"]');
    await reminderQuantity.fill("3");
    await page.locator("h1").click();
    await page.clock.fastForward(120_000);
    await new Promise(resolve => setTimeout(resolve, 250));
    assert.equal(navigations, 0, "An edited form must not reload even after focus leaves it.");
    assert.equal(await reminderQuantity.inputValue(), "3");
    page.off("framenavigated", countNavigation);
  });

  await check("all four milestone routes fit desktop and mobile", async () => {
    for (const width of [375, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      for (const route of ["/saved", "/alerts", "/budget", "/reminders?code=E2E-MILK"]) {
        await go(route);
        assert.equal(await page.locator("h1").count(), 1, route);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true,
          `Horizontal overflow at ${width}px on ${route}`);
        if (route === "/alerts") {
          assert.equal(await page.locator(".alert-retailers dd").evaluateAll(prices => prices.every(price => {
            const range = document.createRange();
            range.selectNodeContents(price);
            return range.getClientRects().length === 1;
          })), true, `Short alert prices wrapped at ${width}px`);
        }
        await capture(route.slice(1).split("?")[0] + (width === 375 ? "-mobile" : ""));
      }
    }
  });

  await check("milestone pages and a budget form work without JavaScript", async () => {
    const nativeContext = await browser.newContext({ javaScriptEnabled: false });
    try {
      const nativePage = await nativeContext.newPage();
      for (const route of ["/saved", "/alerts", "/budget", "/reminders"]) {
        assert.equal((await nativePage.goto(base + route)).status(), 200);
        assert.equal(await nativePage.locator("h1").count(), 1);
      }
      await nativePage.goto(base + "/budget");
      await nativePage.locator('[name="limit"]').fill("20.00");
      await Promise.all([
        nativePage.waitForNavigation(),
        nativePage.getByRole("button", { name: "Save budget" }).click()
      ]);
      assert.match(await nativePage.locator("main").innerText(), /within your planned limit/);
    } finally {
      await nativeContext.close();
    }
  });

  await check("no browser script or console errors", async () => assert.deepEqual(browserErrors, []));
  console.log(`${passed} milestone browser scenarios passed. Artifacts: ${path.relative(root, artifacts)}`);
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
