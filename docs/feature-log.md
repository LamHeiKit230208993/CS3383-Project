# Feature log

Running log of what the app actually does, in build order. A job appears here only after it is
implemented and verified — a plan entry in [Plan/features.md](Plan/features.md) is not evidence.

| Date | Job | Status |
|---|---|---|
| 2026-10-21 | J01 Record fields + H2 file storage | done |
| 2026-10-21 | J02 Sample/daily import with validation | done |
| 2026-10-21 | J03 Search and exact-product comparison | done |
| 2026-10-21 | J04 List editing and screens | done |

---

## 2026-10-21 — J01: record fields and H2 storage

- Domain types `LocalisedName`, `Product`, `Listing`, `Catalogue`, `Comparison`, `ShoppingList`,
  `ListItem` in `com.pricewatch.domain`.
- Spring Data JPA over an H2 file database at `./data/pricewatch`; tests use in-memory H2.
- `spring.jpa.open-in-view=false`; catalogue always fetched with products
  (`CatalogueRepository.findWithProducts()`).
- `price` is nullable so an unknown price stays unknown and is never stored as `0`.

Check: save a product and a list, restart, read both back (see `CataloguePersistenceTest`).

## 2026-10-21 — J02: import sample, then daily JSON

- `POST /import/sample` reads `resources/samples/pricewatch-products.json`.
- `POST /import/daily` downloads `pricewatch.daily-url` (2,735 products), falling back to the sample
  when the URL is unset.
- Whole document parsed and validated first, then one transactional replace. A rejected import
  leaves the last accepted catalogue in place.
- Rejections: not JSON, not an array, empty, missing/duplicate `code`, missing any of the three
  labels, missing category label, repeated retailer, offers without price rows.
- Money parsed as `BigDecimal` from a string; offer text kept verbatim (never divided into a unit
  price). `importedAt` = retrieval time; `sourceDate` stays `null` ("source date unknown").
- Re-import does not duplicate listings.

Check: sample imports; invalid/empty responses keep the old catalogue; repeated import does not
duplicate (`CatalogueImporterTest`).

## 2026-10-21 — J03: search and compare

- `Catalogue.find(query)` case-insensitive substring match over brand, all three names, all three
  category levels. `lang` only selects the displayed label.
- `Product.compare(importedAt)` sorts known prices ascending and marks every retailer at the lowest
  price as a tie.
- Missing price goes to `missing` and can never win, even as the only listing.
- Different codes stay separate products; the three names of one product find the same code.

Check: three names of one product find the same code; different packs stay separate; missing price
never wins (`CatalogueSearchTest`, `ComparisonTest`).

## 2026-10-21 — J04: list editing and screens

- `POST /list/items` (code, quantity). Quantity < 1 or unknown code leaves the list unchanged.
  Adding an existing product increases its quantity instead of adding a second row.
- `POST /list/items/{code}/quantity` replaces quantity; `/toggle` flips checked;
  `/remove` deletes the row.
- List stored in the same H2 file, so items, quantities and checked state survive a restart.
- Screens: `market-desk.html` (Discover), `compare.html`, `list.html`. Saved / Updates / Ask remain
  sample scenes, not backed by data.
- No totals or budgets yet (J07). Unknown prices read "No listed price", never `$0.00`.

Check: full flow in the browser, then restart and read the same list.

---

## Verified state

`.\mvnw.cmd test` — 39 tests passing (21 October 2026):
`CataloguePersistenceTest` 3, `CatalogueSearchTest` 6, `ComparisonTest` 4, `CatalogueImporterTest` 12,
`ListFlowTest` 7, `PageControllerTest` 7.

Browser check against the real 2,735-product daily file: `?q=鮮牛奶` returns 50 results, compare shows
AEON HK$6.00 lowest with PARKNSHOP's `Buy 2 to save $3.00` kept as text, and a 3-pack ticked item is
restored after restart.

Details: [implementation-baseline-j01-j04.md](implementation-baseline-j01-j04.md).

## Not implemented yet

J05 wishlist/tracking/history, J06 alerts, J07 totals/budgets, J08 reminders, J09 chat, J10 list
sharing, J11 offline read-only, J12 languages/accessibility/data controls/history views.
