"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const { mergeCoverage, shopperCoverage } = require("./javascript-coverage.cjs");

function cartRanges(port, ranges) {
  return [{
    url: `http://127.0.0.1:${port}/js/cart.js`,
    source: "0123456789",
    functions: ranges.map(([startOffset, endOffset, count]) => ({
      ranges: [{ startOffset, endOffset, count }]
    }))
  }];
}

test("resolves nested zero-count ranges before merging separate executions", () => {
  const scripts = new Map();
  mergeCoverage(cartRanges(1234, [[0, 10, 1], [2, 8, 0], [4, 5, 1]]), scripts);
  assert.deepEqual([...scripts.get("cart.js").used], [1, 1, 0, 0, 1, 0, 0, 0, 1, 1]);
  mergeCoverage(cartRanges(2345, [[0, 10, 1], [0, 6, 0]]), scripts);
  assert.deepEqual([...scripts.get("cart.js").used], [1, 1, 0, 0, 1, 0, 1, 1, 1, 1]);
});

test("rejects an instrumented report when a required script is missing", () => {
  assert.throws(() => shopperCoverage(true).write("unused.json", "fixture", 0),
    /No coverage collected for cart.js/);
});
