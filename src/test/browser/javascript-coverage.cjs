"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const { pathToFileURL } = require("node:url");

const assets = new Map([
  ["/js/cart.js", "cart.js"],
  ["/js/ask-chat.js", "ask-chat.js"]
]);
const offlineScript = "offline-checklist.html inline script";

function mergeCoverage(entries, scriptSources, offlineFile) {
  const offlineUrl = offlineFile && pathToFileURL(offlineFile).href;
  for (const entry of entries) {
    const url = new URL(entry.url);
    const name = entry.url === offlineUrl ? offlineScript : assets.get(url.pathname);
    if (!name) continue;
    assert.equal(typeof entry.source, "string", `No source available for ${name}`);
    if (name === offlineScript) {
      const html = fs.readFileSync(offlineFile, "utf8").replace(/\r\n?/g, "\n");
      const scripts = [...html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)];
      assert.equal(scripts.length, 1, "The standalone checklist should contain one script");
      assert.equal(entry.source, scripts[0][1], "Measure the inline script alone, excluding the HTML and nonce");
    }
    if (!scriptSources.has(name)) {
      scriptSources.set(name, { source: entry.source, used: new Uint8Array(entry.source.length) });
    }
    const script = scriptSources.get(name);
    assert.equal(script.source, entry.source, `Source changed between ${name} executions`);

    // Inner V8 blocks and functions override enclosing ranges for this execution.
    // Union the resolved results across pages only after applying zero-count ranges.
    const ranges = entry.functions.flatMap(functionCoverage => functionCoverage.ranges)
      .sort((a, b) => (b.endOffset - b.startOffset) - (a.endOffset - a.startOffset));
    const used = new Uint8Array(script.source.length);
    for (const { startOffset, endOffset, count } of ranges) {
      assert.ok(startOffset >= 0 && endOffset <= script.source.length && endOffset >= startOffset,
        `Invalid ${name} source offsets`);
      used.fill(count > 0 ? 1 : 0, startOffset, endOffset);
    }
    used.forEach((executed, offset) => { if (executed) script.used[offset] = 1; });
  }
}

function shopperCoverage(enabled) {
  const scriptSources = new Map();
  return {
    async start(page) {
      if (enabled) await page.coverage.startJSCoverage({ resetOnNavigation: false });
    },
    async collect(page, offlineFile) {
      if (enabled) mergeCoverage(await page.coverage.stopJSCoverage(), scriptSources, offlineFile);
    },
    write(output, browserVersion, scenariosPassed) {
      if (!enabled) return;
      const scripts = [...assets.values(), offlineScript].map(name => {
        const script = scriptSources.get(name);
        assert.ok(script, `No coverage collected for ${name}`);
        const executed = script.used.reduce((total, value) => total + value, 0);
        assert.ok(executed > 0, `No execution ranges collected for ${name}`);
        return {
          name, executed, total: script.source.length,
          percent: Math.round(executed * 10000 / script.source.length) / 100
        };
      });
      fs.writeFileSync(output, JSON.stringify({
        metric: "V8 executed JavaScript source ranges in UTF-16 code units",
        browserVersion, scenariosPassed, scripts
      }, null, 2) + "\n");
    }
  };
}

module.exports = { mergeCoverage, shopperCoverage };
