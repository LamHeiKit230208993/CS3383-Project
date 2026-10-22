# Test Case Catalog

Use this brief index to review the automated test suite. Keep one detailed
document per test case group in [`cases/`](./cases/).

The suite covers the baseline jobs J01-J04. See
[the implementation record](../implementation-baseline-j01-j04.md) for the
behaviour each case is protecting.

## Storage — J01

| ID | Area | Test class | Tests | Expected result | Source |
|---|---|---|---|---|---|
| [TC-DATA-001](./cases/TC-DATA-001.md) | H2 storage | `CataloguePersistenceTest` | 3 | Saved catalogue and list read back after the context is cleared; unknown prices stay `null` | [`CataloguePersistenceTest.java`](../../src/test/java/com/pricewatch/domain/CataloguePersistenceTest.java) |

## Import — J02

| ID | Area | Test class | Tests | Expected result | Source |
|---|---|---|---|---|---|
| [TC-IMP-001](./cases/TC-IMP-001.md) | Sample import | `CatalogueImporterTest` | 6 | Three products, three language names per code, packs stay separate, offer text kept whole, retrieval time recorded and source date unknown, repeat import adds no listings | [`CatalogueImporterTest.java`](../../src/test/java/com/pricewatch/importing/CatalogueImporterTest.java) |
| [TC-IMP-002](./cases/TC-IMP-002.md) | Rejected import | `CatalogueImporterTest` | 6 | Invalid JSON, empty array, missing labels, duplicate code, and unparseable or absent price never replace the accepted catalogue; price stays unknown, never `0` | [`CatalogueImporterTest.java`](../../src/test/java/com/pricewatch/importing/CatalogueImporterTest.java) |

## Search and comparison — J03

| ID | Area | Test class | Tests | Expected result | Source |
|---|---|---|---|---|---|
| [TC-SRCH-001](./cases/TC-SRCH-001.md) | Search | `CatalogueSearchTest` | 6 | Three names of one product return one code; brand, category and name match; case and spaces ignored; blank query returns nothing; different packs stay separate | [`CatalogueSearchTest.java`](../../src/test/java/com/pricewatch/domain/CatalogueSearchTest.java) |
| [TC-CMP-001](./cases/TC-CMP-001.md) | Comparison | `ComparisonTest` | 4 | Lowest price wins, equal prices tie, a `null` price goes to `missing` and never wins, all-missing has no winner | [`ComparisonTest.java`](../../src/test/java/com/pricewatch/domain/ComparisonTest.java) |

## List — J04

| ID | Area | Test class | Tests | Expected result | Source |
|---|---|---|---|---|---|
| [TC-LIST-001](./cases/TC-LIST-001.md) | List edits | `ListFlowTest` | 10 | Add, repeat-add merges quantity, invalid quantity and unknown product change nothing, tick and quantity persist, remove deletes only the named row, missing items are rejected | [`ListFlowTest.java`](../../src/test/java/com/pricewatch/web/ListFlowTest.java) |

## Web pages — J04

| ID | Area | Test class | Tests | Expected result | Source |
|---|---|---|---|---|---|
| [TC-WEB-001](./cases/TC-WEB-001.md) | Discover page | `PageControllerTest` | 3 | Imported products listed with retrieval time and "source date unknown"; search finds by Traditional and Simplified Chinese name | [`PageControllerTest.java`](../../src/test/java/com/pricewatch/web/PageControllerTest.java) |
| [TC-WEB-002](./cases/TC-WEB-002.md) | Compare page | `PageControllerTest` | 3 | Ties and offer text shown with retrieval time; unknown code redirects; an unknown price reads "No price listed" and never `HK$0.00` | [`PageControllerTest.java`](../../src/test/java/com/pricewatch/web/PageControllerTest.java) |
| [TC-WEB-003](./cases/TC-WEB-003.md) | List page | `PageControllerTest` | 3 | Empty list renders its empty state; a row shows its per-pack amount and the cost for its quantity; every row has a remove control | [`PageControllerTest.java`](../../src/test/java/com/pricewatch/web/PageControllerTest.java) |
| [TC-WEB-004](./cases/TC-WEB-004.md) | List route visibility | Browser check | — | `market-desk.js` does not hide the server-rendered `/list` page | [`market-desk.js`](../../src/main/resources/static/js/market-desk.js) |
| [TC-WEB-005](./cases/TC-WEB-005.md) | Compare route visibility | Browser check | — | `market-desk.js` does not hide the server-rendered `/compare/{code}` page | [`market-desk.js`](../../src/main/resources/static/js/market-desk.js) |

## Review status

- Current automated cases: **44 test methods** in six test classes
- Current coverage: H2 storage, import validation, search, comparison, list
  edits (including remove), and the three server-rendered pages
- Not covered by automated tests: JavaScript behaviour, visual layout,
  keyboard and screen-reader behaviour, and the live daily download
- Test execution results must come from the current Maven output. The tables
  define the checks; dated records below state the observed run results.

## Baseline check record — 21 October 2026

- Full suite: `.\mvnw.cmd -o test` passed **44 tests**, with no failures,
  errors, or skipped tests (44 test methods, of which Surefire also counts
  two `@BeforeEach` setups).
- Per class: `CataloguePersistenceTest` 3, `CatalogueSearchTest` 6,
  `ComparisonTest` 4, `CatalogueImporterTest` 12, `ListFlowTest` 10,
  `PageControllerTest` 9.
- Browser checks on `localhost:8080` used the **real** daily file (2,735
  products), not only the sample:
  - sample import loaded 3 products; the daily import replaced them with 2,735
  - `?q=鮮牛奶` returned 50 results with `P000000124` first
  - `/compare/P000000124` showed AEON at HK$6.00 as lowest, and PARKNSHOP
    HK$9.00 kept its `Buy 2 to save $3.00` text
  - adding 3 packs and ticking the item gave `1 items · 1 checked`
  - after stopping and restarting the app, `/list` still showed 1 item,
    3 packs, checked
  - a 3-pack row showed `HK$7.50 × 3` and `HK$22.50`; removing it left the
    other row, and changing the remaining row to 4 packs showed `HK$46.00`
- The earlier prototype check record (17 October 2026, 62 browser checks,
  `HelloControllerTest`) is superseded: that controller and test class were
  replaced by the J01-J04 implementation.
- Limits: the browser checks used one Chrome instance. Scripted checks are not
  a full accessibility audit. These results do not prove scheduled daily
  imports, alerts, chat, or history; those are later jobs.
