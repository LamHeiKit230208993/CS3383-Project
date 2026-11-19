"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const http = require("node:http");
const path = require("node:path");
const { spawn } = require("node:child_process");
const { once } = require("node:events");
const { pathToFileURL } = require("node:url");
const { chromium } = require("playwright");

const root = path.resolve(__dirname, "../../..");
const jar = path.join(root, "target/pricewatch-0.0.1-SNAPSHOT.jar");
const artifacts = path.join(root, "target/browser-milestone");
const label = value => ({ en: value, "zh-Hant": value, "zh-Hans": value });
let milkPrice = "10.00";
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
      prices: [{ supermarketCode: "TEST-STORE", price: milkPrice }], offers: []
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

  await check("wishlist save is separate from tracking", async () => {
    await go("/saved?code=E2E-MILK");
    await submit(page.getByRole("button", { name: "Save to wishlist" }), "/saved");
    assert.match(await page.locator("main").innerText(), /E2E-MILK/);
    assert.match(await page.locator("main").innerText(), /No tracking yet/);
    await go("/saved?code=E2E-MILK");
    await submit(page.locator('section[aria-labelledby="save-product-title"]').getByRole("button", { name: "Track this product" }), "/saved");
    assert.match(await page.locator("main").innerText(), /TEST-STORE · E2E-MILK/);
  });

  await check("live target and drop alerts use changed source prices", async () => {
    await go("/alerts");
    await page.locator('[action="/alerts/settings"] [name="quietStart"]').fill("00:00");
    await page.locator('[action="/alerts/settings"] [name="quietEnd"]').fill("00:00");
    await page.locator('[action="/alerts/settings"] [name="summaryTime"]').fill("00:00");
    await submit(page.getByRole("button", { name: "Save timing" }), "/alerts");
    assert.equal(await page.locator('[action="/alerts/settings"] [name="quietStart"]').inputValue(), "00:00");
    assert.equal(await page.locator('[action="/alerts/settings"] [name="quietEnd"]').inputValue(), "00:00");

    await page.locator('[action="/alerts/target"] [name="targetPrice"]').fill("9.00");
    await submit(page.getByRole("button", { name: "Save target" }), "/alerts");
    await page.locator('[action="/alerts/drop"] [name="enabled"]').check();
    await submit(page.getByRole("button", { name: "Save drop rule" }), "/alerts");

    milkPrice = "8.00";
    await go("/data");
    await submit(page.getByRole("button", { name: "Refresh live prices" }), "/data");
    await go("/alerts");
    assert.match(await page.locator("main").innerText(), /TARGET PRICE/);
    assert.match(await page.locator("main").innerText(), /PRICE DROP/);
    assert.match(await page.locator("main").innerText(), /Previous: HK\$10\.00/);
    assert.match(await page.locator("main").innerText(), /Current: HK\$8\.00/);
    await capture("alerts");
  });

  await check("reminder create, add, clear, and snooze keep their separate effects", async () => {
    await go("/products/E2E-NOODLE");
    await page.getByRole("link", { name: "Set a replenishment reminder" }).click();
    await page.waitForURL(url => url.pathname === "/reminders");
    await page.locator("#reminder-quantity").fill("2");
    await page.locator("#reminder-interval").fill("7");
    await page.locator("#reminder-retailer").selectOption("NOODLE-STORE");
    await submit(page.getByRole("button", { name: "Save reminder" }), "/reminders");
    const reminder = page.locator("article.list-item").filter({ hasText: "E2E-NOODLE" });
    assert.match(await reminder.innerText(), /2 packs · every 7 days/);
    await submit(reminder.getByRole("button", { name: /^Add 2 packs of .* to My list$/ }), "/reminders");
    assert.match(await page.locator("main").innerText(), /The reminder is still active/);
    await go("/list");
    assert.match(await page.locator("main").innerText(), /E2E-NOODLE/);
    await go("/reminders");
    const currentReminder = page.locator("article.list-item").filter({ hasText: "E2E-NOODLE" });
    await submit(currentReminder.getByRole("button", { name: "Clear" }), "/reminders");
    assert.match(await page.locator("main").innerText(), /next due date uses the interval from now/);
    const clearedReminder = page.locator("article.list-item").filter({ hasText: "E2E-NOODLE" });
    await submit(clearedReminder.getByRole("button", { name: /^Snooze reminder/ }), "/reminders");
    assert.match(await page.locator("main").innerText(), /snoozed/);
    await capture("reminders");
  });

  await check("budget reflects the known current list estimate", async () => {
    await go("/products/E2E-MILK");
    await page.locator(".retailer-option").first().click();
    await page.locator("#quantity").fill("1");
    await submit(page.getByRole("button", { name: "Add to my list" }), "/list");
    await go("/budget");
    await page.locator('[name="period"]').selectOption("MONTHLY");
    await page.locator('[name="limit"]').fill("15.00");
    await submit(page.getByRole("button", { name: "Save budget" }), "/budget");
    assert.match(await page.locator("main").innerText(), /known subtotal is already above your limit/i);
    assert.match(await page.locator("main").innerText(), /Known estimate by category/);
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
  });

  await check("ask uses checked facts with a safe no-AI fallback", async () => {
    await go("/ask");
    await page.locator("#ask-question").fill("Price for E2E-MILK");
    await submit(page.getByRole("button", { name: "Ask" }), "/ask");
    const answer = await page.locator("main").innerText();
    assert.match(answer, /CHECKED ANSWER/);
    assert.match(answer, /E2E-MILK/);
    assert.match(answer, /TEST-STORE/);
    assert.match(answer, /HK\$7\.00/);
    assert.match(answer, /source price date/i);

    await page.locator("#ask-question").fill("Compare E2E-MILK and E2E-NOODLE");
    await submit(page.getByRole("button", { name: "Ask" }), "/ask");
    assert.match(await page.locator("main").innerText(), /Choose the product/);
    await page.locator('input[name="productCode"][value="E2E-NOODLE"]').check();
    await submit(page.getByRole("button", { name: "Use this product" }), "/ask");
    assert.match(await page.locator("main").innerText(), /HK\$5\.00/);
    await capture("ask");
  });

  await check("list export is a snapshot and import creates a separate validated list", async () => {
    const exported = await context.request.get(`${base}/list/export`);
    assert.equal(exported.status(), 200);
    assert.match(exported.headers()["content-disposition"], /attachment/);
    const snapshot = await exported.json();
    assert.equal(snapshot.format, "pricewatch-list");
    assert.equal(snapshot.version, 1);
    assert.equal(snapshot.name, "My list");
    assert.equal(snapshot.items.some(item => item.productCode === "E2E-MILK"
      && item.quantity === 1 && item.retailerCode === "TEST-STORE"), true);

    await go("/list/import");
    await page.locator("#share-file").setInputFiles({
      name: "trip.json",
      mimeType: "application/json",
      buffer: Buffer.from(JSON.stringify({
        format: "pricewatch-list",
        version: 1,
        name: "Trip",
        items: [{ productCode: "E2E-NOODLE", quantity: 3, checked: true, retailerCode: "NOODLE-STORE" }]
      }))
    });
    await submit(page.getByRole("button", { name: "Import as a new list" }), "/list/import");
    const imported = await page.locator("main").innerText();
    assert.match(imported, /Imported Trip as a new list\./);
    assert.match(imported, /Separate list/);
    assert.match(imported, /E2E-NOODLE/);
    assert.match(imported, /NOODLE-STORE/);
    assert.match(imported, /My list/);
    const importedRow = page.locator("table.facts-table tbody tr").filter({ hasText: "E2E-NOODLE" });
    assert.match(await importedRow.innerText(), /\b3\b/);
    assert.match(await importedRow.innerText(), /Yes/);

    await go("/list");
    const mainNoodle = page.locator("article.list-item").filter({ hasText: "E2E-NOODLE" });
    assert.equal(await mainNoodle.count(), 1);
    assert.equal(await mainNoodle.locator('[name="quantity"]').inputValue(), "2");
    assert.equal(await mainNoodle.locator('[name="checked"]').isChecked(), false);
    assert.match(await page.locator("main").innerText(), /E2E-MILK/);

    await go("/list/import");
    await page.locator("#share-file").setInputFiles({
      name: "broken.json",
      mimeType: "application/json",
      buffer: Buffer.from("{not json")
    });
    await submit(page.getByRole("button", { name: "Import as a new list" }), "/list/import");
    const rejected = await page.locator("main").innerText();
    assert.match(rejected, /not a readable list file/);
    assert.match(rejected, /Trip/);

    for (const width of [375, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      await go("/list/import");
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true,
        `List transfer overflow at ${width}px`);
      await capture(`list-import-${width}`);
    }
  });

  await check("invalid form parameters return client errors or validation redirects, never 500", async () => {
    const invalidReminder = await context.request.post(`${base}/reminders/create`, {
      form: { code: "E2E-NOODLE", quantity: "0", intervalDays: "7" }, maxRedirects: 0
    });
    assert.equal(invalidReminder.status(), 302);
    await go("/alerts");
    const code = await page.locator('[action="/alerts/target"] [name="code"]').inputValue();
    const invalidTarget = await context.request.post(`${base}/alerts/target`, {
      form: { code, targetPrice: "not-a-price" }, maxRedirects: 0
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
    let navigations = 0;
    const countNavigation = frame => { if (frame === page.mainFrame()) navigations++; };
    page.on("framenavigated", countNavigation);
    await page.locator("#reminder-quantity").fill("3");
    await page.locator("h1").click();
    await page.clock.fastForward(120_000);
    await new Promise(resolve => setTimeout(resolve, 250));
    assert.equal(navigations, 0, "An edited form must not reload even after focus leaves it.");
    assert.equal(await page.locator("#reminder-quantity").inputValue(), "3");
    page.off("framenavigated", countNavigation);
  });

  await check("all four milestone routes fit desktop and mobile", async () => {
    for (const width of [375, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      for (const route of ["/saved", "/alerts", "/budget", "/reminders"]) {
        await go(route);
        assert.equal(await page.locator("h1").count(), 1, route);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true,
          `Horizontal overflow at ${width}px on ${route}`);
        if (route === "/alerts") {
          assert.equal(await page.locator(".list-item-heading > strong").evaluateAll(prices => prices.every(price => {
            const range = document.createRange();
            range.selectNodeContents(price);
            return range.getClientRects().length === 1;
          })), true, `Short alert prices wrapped at ${width}px`);
        }
        await capture(route.slice(1) + (width === 375 ? "-mobile" : ""));
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

  await check("saved language and high contrast apply to every page", async () => {
    await go("/preferences");
    await page.locator('main form[action="/preferences"] [name="locale"][value="zh-HK"]').check();
    await page.locator('main form[action="/preferences"] [name="contrast"][value="high"]').check();
    await submit(page.locator('main form[action="/preferences"] button[type="submit"]'), "/preferences");

    await go("/budget");
    assert.equal(await page.locator("html").getAttribute("lang"), "zh-Hant");
    assert.equal(await page.locator("body").getAttribute("data-contrast"), "high");
    assert.match(await page.locator("main").innerText(), /你的計劃上限/);

    const contrastRatio = await page.evaluate(() => {
      const rgb = value => value.match(/[\d.]+/g).slice(0, 3).map(Number);
      const luminance = ([r, g, b]) => {
        const channel = value => {
          const scaled = value / 255;
          return scaled <= 0.03928 ? scaled / 12.92 : ((scaled + 0.055) / 1.055) ** 2.4;
        };
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
      };
      const style = getComputedStyle(document.body);
      const foreground = luminance(rgb(style.color));
      const background = luminance(rgb(style.backgroundColor));
      return (Math.max(foreground, background) + 0.05) / (Math.min(foreground, background) + 0.05);
    });
    assert.ok(contrastRatio >= 7, `High-contrast body text ratio was ${contrastRatio.toFixed(2)}:1`);

    await go("/saved");
    const pillBorder = await page.locator(".status-pill").first()
      .evaluate(element => getComputedStyle(element).borderTopWidth);
    assert.notEqual(pillBorder, "0px", "Status must not rely on colour alone in high contrast.");

    await go("/preferences");
    for (let press = 0; press < 25; press++) {
      await page.keyboard.press("Tab");
      if (await page.evaluate(() => !!document.activeElement.closest("main"))) break;
    }
    const focus = await page.evaluate(() => ({
      inMain: !!document.activeElement.closest("main"),
      outlineWidth: getComputedStyle(document.activeElement).outlineWidth
    }));
    assert.equal(focus.inMain, true, "Tab must reach the main content.");
    assert.equal(focus.outlineWidth, "4px", "High-contrast keyboard focus must be clearly visible.");

    for (const width of [375, 768, 1024, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      await go("/budget");
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true,
        `Chinese layout overflow at ${width}px`);
      await capture(`budget-zh-hant-${width}`);
    }

    await page.setViewportSize({ width: 1440, height: 1000 });
    await go("/preferences");
    await page.locator('main form[action="/preferences"] [name="locale"][value="en"]').check();
    await page.locator('main form[action="/preferences"] [name="contrast"][value="normal"]').check();
    await submit(page.locator('main form[action="/preferences"] button[type="submit"]'), "/preferences");
    await go("/budget");
    assert.equal(await page.locator("html").getAttribute("lang"), "en");
    assert.equal(await page.locator("body").getAttribute("data-contrast"), "normal");
  });

  await check("history shows the accepted observations for a tracked product", async () => {
    await go("/saved");
    assert.match(await page.locator("main").innerText(), /View history/);
    await Promise.all([
      page.waitForURL(url => url.pathname === "/history/E2E-MILK"),
      page.getByRole("link", { name: "View history" }).first().click()
    ]);
    const history = await page.locator("main").innerText();
    assert.match(history, /History for Fresh Milk 236mL/);
    assert.match(history, /TEST-STORE/);
    assert.match(history, /Retrieved/);
    assert.match(history, /Listed price/);
    assert.match(history, /HK\$\d+\.\d{2}/);
    assert.match(history, /90-day retention/);
    assert.match(history, /daily values are not invented/);

    const missing = await context.request.get(`${base}/history/UNKNOWN`, { maxRedirects: 0 });
    assert.equal(missing.status(), 404);

    for (const width of [375, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      await go("/history/E2E-MILK");
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true,
        `History overflow at ${width}px`);
      await capture(`history-${width}`);
    }
    await page.setViewportSize({ width: 1440, height: 1000 });
  });

  await check("privacy exports personal records, then deletes only those records", async () => {
    await go("/privacy");
    const privacy = await page.locator("main").innerText();
    assert.match(privacy, /Privacy and your local data/);
    assert.match(privacy, /What is stored now/);
    assert.match(privacy, /Download personal data/);
    assert.match(privacy, /I understand this permanently deletes/);
    await capture("privacy");

    const exported = await context.request.get(`${base}/privacy/export`);
    assert.equal(exported.status(), 200);
    assert.match(exported.headers()["content-disposition"], /attachment/);
    const data = await exported.json();
    assert.equal(data.format, "pricewatch-personal-data");
    assert.equal(data.version, 1);
    assert.equal(data.lists.some(list => list.items.some(item => item.productCode === "E2E-MILK")), true);
    assert.equal(data.wishlist.some(entry => entry.productCode === "E2E-MILK"), true);
    assert.equal(data.tracking.some(period => period.productCode === "E2E-MILK"
      && period.observations.length >= 2), true);
    assert.equal(data.alerts.length > 0, true);
    assert.equal(data.budget.period, "MONTHLY");
    assert.equal(data.reminders.length > 0, true);
    assert.equal(data.preferences.locale, "en");
    assert.equal("products" in data, false);
    assert.equal("catalogue" in data, false);

    const cancelled = await context.request.post(`${base}/privacy/delete`, { maxRedirects: 0 });
    assert.equal(cancelled.status(), 302);
    await go("/privacy");
    assert.match(await page.locator("main").innerText(), /Confirm the deletion first/);
    assert.equal(await page.locator(".privacy-facts dd").first().innerText(), "2");

    await page.locator('form[action="/privacy/delete"] [name="confirmDelete"]').check();
    await submit(page.getByRole("button", { name: "Delete personal records" }), "/privacy");
    assert.match(await page.locator("main").innerText(), /Personal records were deleted/);
    const facts = await page.locator(".privacy-facts dd").allInnerTexts();
    assert.deepEqual(facts.slice(0, 7), ["0", "0", "0", "0", "0", "0", "0"]);
    assert.equal(facts.slice(7).every(value => value === "Not set"), true);

    await go("/products/E2E-MILK");
    assert.match(await page.locator("main").innerText(), /Fresh Milk 236mL/);

    for (const [code, quantity] of [["E2E-MILK", "1"], ["E2E-NOODLE", "2"]]) {
      await go(`/products/${code}`);
      await page.locator(".retailer-option").first().click();
      await page.locator("#quantity").fill(quantity);
      await submit(page.getByRole("button", { name: "Add to my list" }), "/list");
    }
    await go("/list");
    const list = await page.locator("main").innerText();
    assert.match(list, /E2E-MILK/);
    assert.match(list, /E2E-NOODLE/);
  });

  await check("offline snapshot opens from a file with the app stopped and no live resources", async () => {
    const response = await context.request.get(`${base}/list/offline`);
    assert.equal(response.status(), 200);
    assert.match(response.headers()["content-disposition"], /attachment/);
    assert.match(response.headers()["content-type"], /text\/html/);
    const html = await response.body();
    const file = path.join(artifacts, "my-list.pricewatch.html");
    fs.writeFileSync(file, html);
    const sourceText = html.toString("utf8");
    assert.match(sourceText, /E2E-MILK/);
    assert.match(sourceText, /E2E-NOODLE/);
    assert.match(sourceText, /HK\$7\.00/);
    assert.match(sourceText, /HK\$10\.00/);
    assert.match(sourceText, /Source price date/);
    assert.doesNotMatch(sourceText, /<script|<form|<a\s|href=|src=|https?:\/\//i);

    const stopped = once(child, "exit");
    child.kill();
    await stopped;

    const offlineContext = await browser.newContext({
      viewport: { width: 1440, height: 1000 },
      offline: true
    });
    try {
      const offlinePage = await offlineContext.newPage();
      await offlinePage.goto(pathToFileURL(file).href);
      const bodyText = await offlinePage.locator("body").innerText();
      assert.match(bodyText, /E2E-MILK/);
      assert.match(bodyText, /E2E-NOODLE/);
      assert.match(bodyText, /TEST-STORE/);
      assert.match(bodyText, /NOODLE-STORE/);
      assert.match(bodyText, /HK\$7\.00/);
      assert.match(bodyText, /HK\$10\.00/);
      assert.match(bodyText, /Saved on/i);
      assert.match(bodyText, /Data age/i);
      assert.match(bodyText, /Source price date/i);
      assert.match(bodyText, /Unknown/);
      assert.match(bodyText, /Read-only/i);
      assert.deepEqual(await offlinePage.evaluate(() => ({
        scripts: document.querySelectorAll("script").length,
        forms: document.querySelectorAll("form").length,
        links: document.querySelectorAll("a").length,
        resources: document.querySelectorAll("[href], [src]").length
      })), { scripts: 0, forms: 0, links: 0, resources: 0 });
      assert.equal(await offlinePage.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
        true, "Offline snapshot overflow at 1440px");
      await offlinePage.screenshot({
        path: path.join(artifacts, "offline-list.png"), fullPage: true, animations: "disabled"
      });

      await offlinePage.setViewportSize({ width: 375, height: 1000 });
      assert.equal(await offlinePage.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
        true, "Offline snapshot overflow at 375px");
      await offlinePage.screenshot({
        path: path.join(artifacts, "offline-list-375.png"), fullPage: true, animations: "disabled"
      });
    } finally {
      await offlineContext.close();
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
