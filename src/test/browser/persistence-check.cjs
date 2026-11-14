"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { spawn } = require("node:child_process");
const { once } = require("node:events");
const { chromium } = require("playwright");

const root = path.resolve(__dirname, "../../..");
const currentJar = path.join(root, "target/pricewatch-0.0.1-SNAPSHOT.jar");
const baselineJar = path.resolve(process.env.BASELINE_JAR || currentJar);
const databaseFolder = fs.mkdtempSync(path.join(os.tmpdir(), "pricewatch-persistence-"));
const database = path.join(databaseFolder, "catalogue").replaceAll("\\", "/");
let app;
let browser;

async function stopApp() {
  if (app && app.exitCode === null && app.signalCode === null) {
    const exited = once(app, "exit");
    app.kill();
    await exited;
  }
}

async function startApp(jar) {
  assert.ok(fs.existsSync(jar), `Missing packaged app: ${jar}`);
  const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, "bin/java.exe") : "java";
  app = spawn(java, ["-jar", jar, "--server.port=0", "--server.address=127.0.0.1",
    // Windows child.kill terminates the process without a graceful Java shutdown.
    `--spring.datasource.url=jdbc:h2:file:${database};DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0`,
    "--pricewatch.jobs.enabled=false"], { cwd: root, windowsHide: true, stdio: ["ignore", "pipe", "pipe"] });
  return new Promise((resolve, reject) => {
    let output = "";
    const timer = setTimeout(() => reject(new Error(`App startup timed out: ${output}`)), 45000);
    const read = chunk => {
      output += chunk.toString();
      const port = /Tomcat started on port (\d+)/.exec(output)?.[1];
      if (port && output.includes("Started PricewatchApplication")) {
        clearTimeout(timer);
        resolve(`http://127.0.0.1:${port}`);
      }
    };
    app.stdout.on("data", read);
    app.stderr.on("data", read);
    app.once("error", error => { clearTimeout(timer); reject(error); });
    app.once("exit", code => { clearTimeout(timer); reject(new Error(`App exited (${code}): ${output}`)); });
  });
}

async function main() {
  browser = await chromium.launch({ channel: process.env.BROWSER_CHANNEL, headless: true });
  const page = await browser.newPage();
  let base = await startApp(baselineJar);
  await page.goto(base + "/data");
  await page.getByRole("button", { name: "Load checked sample", exact: true }).click();
  await page.waitForURL(url => url.pathname.split(";")[0] === "/data");
  await page.goto(base + "/products/P000000124");
  await page.locator(".retailer-choice").filter({
    has: page.getByRole("radio", { name: "Choose JASONS", exact: true })
  }).click();
  await page.locator("#quantity").fill("2");
  await page.getByRole("button", { name: "Add to my list", exact: true }).click();
  await page.waitForURL(url => url.pathname.split(";")[0] === "/list");
  await page.locator("[name=checked]").check();
  await page.getByRole("button", { name: /Save changes for/ }).click();
  await page.waitForLoadState("load");
  assert.equal(await page.locator(".list-item").count(), 1);
  assert.equal(await page.locator("[name=checked]").isChecked(), true);
  await stopApp();

  base = await startApp(currentJar);
  await page.goto(base + "/list");
  assert.equal(await page.locator(".list-item").count(), 1);
  assert.equal(await page.locator("[name=quantity]").inputValue(), "2");
  assert.equal(await page.locator("[name=retailerCode]").inputValue(), "JASONS");
  assert.equal(await page.locator("[name=checked]").isChecked(), true);
  assert.match(await page.locator(".line-total").innerText(), /25\.00/);
  assert.match(await page.locator(".source-banner").innerText(), /Historical sample/);
  console.log("PASS file-database restart/upgrade preserves catalogue, retailer, quantity, checked state, and exact total.");
  console.log(`Isolated database: ${databaseFolder}`);
}

main().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => {
  try { if (browser) await browser.close(); }
  finally { await stopApp(); }
});
