# TC-SRCH-001 — Search matches all stored labels

| Field | Details |
|---|---|
| Test class | `CatalogueSearchTest` |
| Test methods | `threeSourceNamesOfOneProductFindTheSameCode()`, `searchMatchesBrandCategoryAndName()`, `searchIsCaseInsensitiveAndTrims()`, `blankQueryReturnsNothing()`, `differentPackCodesAreNotMerged()`, `unknownCodeReturnsNull()` |
| Type | Plain unit test over an in-memory `Catalogue` |
| Input | Constructed catalogue with three products |

## Purpose

Verify the J03 search rules: one product code behind three language names,
text matching across brand and category, and no merging of different packs.

## Steps

1. Build a catalogue with a 236 mL and a 946 mL milk and one bread.
2. Search each language name and each field.
3. Search with mixed case, padding spaces, and an empty query.

## Expected result

- `Fresh Milk`, `鮮牛奶`, and `鲜牛奶` each return only `P1`.
- `milk` returns both milk codes; `bread` returns the bread; a category name
  returns all three.
- `  MILK  ` behaves the same as `milk`.
- An empty or `null` query returns an empty list rather than everything.
- `milk` returns two separate codes, never one merged product.
- An unknown code returns `null`.

## Limits

Substring matching only. Spelling tolerance, pinyin, and retailer search are
not implemented and not tested; they belong to a later R01 check.

Source: [`CatalogueSearchTest.java`](../../../src/test/java/com/pricewatch/domain/CatalogueSearchTest.java)
