# Baseline J01-J04 — implementation

Record of what was built for [issue #13](https://github.com/LamHeiKit230208993/CS3383-Project/issues/13)
(baseline jobs J01–J04 from [the baseline jobs plan](Plan/features.md)). Diagrams live in
[`Eric/uml/`](Eric/uml/).

## 1. How to run and check

```powershell
.\mvnw.cmd test                  # 39 tests
.\mvnw.cmd spring-boot:run       # http://localhost:8080
```

The H2 file database is created on first run at `./data/pricewatch`. Delete that folder to reset the
app to an empty state. Tests use an in-memory database and never touch the file.

Manual check (the one the issue asks for):

1. Open `http://localhost:8080/` and press **Import sample file**.
2. Search `milk` (or `鮮牛奶`); open a product.
3. Press **Add to list** with a quantity.
4. Open **My list**, change a quantity, press **Tick off**.
5. Stop the app, run it again, open `/list` — the same items, quantities, and checked state come back.

## 2. Records and fields (J01)

All classes are in `com.pricewatch.domain`. They match
[`baseline-j01-j04-class.puml`](Eric/uml/baseline-j01-j04-class.puml).

| Type | Fields | Notes |
|---|---|---|
| `LocalisedName` | `en`, `zhHant`, `zhHans` | One label set per brand, name, and each category level. `text(lang)` picks one. |
| `Product` | `code`, `brand`, `name`, `category1..3`, `listings` | `code` is the primary key — that is what keeps packs separate. |
| `Listing` | `retailerCode`, `price` (nullable), `offer` (original text) | `price == null` means unknown. Never stored as `0`. |
| `Catalogue` | `importedAt`, `sourceDate` (nullable), `sourceUrl`, `products` | One row. `find(query)` and `product(code)` are the search entry points. |
| `Comparison` | `cheapest`, `ties`, `missing`, `importedAt` | Built on demand; not stored. |
| `ShoppingList` | `name`, `items` | One row, with `add`, `changeQuantity`, `toggle`. |
| `ListItem` | `productCode`, `quantity`, `checked`, `addedAt` | Stores the code only — no copied price. |

Storage is Spring Data JPA over H2 file storage. `spring.jpa.open-in-view=false`, so a detached
`Catalogue` is always fetched with its products eagerly (`CatalogueRepository.findWithProducts()`).

## 3. Import rules (J02)

`com.pricewatch.importing.CatalogueImporter`.

- **Sample first, then daily.** `POST /import/sample` reads `src/main/resources/samples/pricewatch-products.json`.
  `POST /import/daily` downloads `pricewatch.daily-url`. Import is user-triggered; there is no scheduler
  in J01–J04.
- **Validate, then replace.** The whole document is parsed and validated before anything is written.
  A rejected import leaves the accepted catalogue untouched. Rejections: not JSON, not an array, empty array,
  missing `code`, duplicate `code`, missing any of the three labels, missing category label, repeated retailer
  inside one product, offers without price rows.
- **Money** is parsed with `BigDecimal` from the string field. A missing or unparseable price is `null`
  (unknown), never `0`.
- **Offer text** is stored as published, per retailer, in three languages. `Buy 3 at $30.00` is never divided
  into a unit price.
- **Dates**: `importedAt` is the app's retrieval time. `sourceDate` stays `null` because the file has no date
  field. The UI says "source date unknown" rather than pretending otherwise.
- **Repeated import does not duplicate**: the replace path deletes the previous catalogue and its products,
  then writes the new one in a single transaction. A failure rolls both back.

## 4. Search and comparison rules (J03)

- `Catalogue.find(query)` is a case-insensitive substring match over brand, all three product names, all three
  category levels. Multi-word and retailer search are **not** supported in this baseline; spelling tolerance
  is a later R01 check.
- `Product.compare()` sorts known prices ascending and marks **every** retailer at the lowest price as a tie.
- A `null` price goes to `missing` and can never win, even when it is the only listing.
- Different `code` values stay separate products. The three names of one product share one code, so all
  three find the same record.

## 5. List behaviour (J04)

- `POST /list/items` with `code` and `quantity`. Quantity below 1, or a code not in the stored catalogue,
  leaves the list unchanged and shows a message.
- Adding a product already on the list adds to its quantity instead of creating a second row.
- `POST /list/items/{code}/quantity` replaces the quantity; `POST /list/items/{code}/toggle` flips the
  checked state.
- The list is stored in the same H2 file, so it survives a restart.
- **No totals and no budget** are shown: list totals are J07. No price is rendered as `0` or `$0.00`;
  unknown prices read "No price listed" / "No listed price".

## 6. Screens

The Market Desk prototype shell is reused. `market-desk.html` serves Discover; `compare.html` and `list.html`
serve their own scenes. Saved, Updates, and Ask remain labelled sample scenes and are not backed by data —
they belong to later jobs.

## 7. Observed test results

Run on 21 October 2026 with `.\mvnw.cmd -o test`:

| Test class | Tests | Result |
|---|---|---|
| `CataloguePersistenceTest` | 3 | pass |
| `CatalogueSearchTest` | 6 | pass |
| `ComparisonTest` | 4 | pass |
| `CatalogueImporterTest` | 12 | pass |
| `ListFlowTest` | 7 | pass |
| `PageControllerTest` | 7 | pass |
| **Total** | **39** | **BUILD SUCCESS** |

Browser verification on `localhost:8080` with the **real** daily file (2,735 products), not just the sample:

| Check | Observed |
|---|---|
| Sample import | 3 products, `Retrieved <time> · source date unknown` |
| Daily import | 2,735 products, replaced the sample |
| `?q=鮮牛奶` | 50 results, `P000000124` first |
| `/compare/P000000124` | AEON HK$6.00 lowest; PARKNSHOP HK$9.00 but `Buy 2 to save $3.00` kept as text |
| Add 3, tick off | `1 items · 1 checked` |
| Restart, open `/list` | `1 items · 1 checked` — product, 3 packs, checked state all restored |

## 8. Known limits

- Import is manual and runs while the app is open. Not scheduled.
- Search is simple text matching; no spelling tolerance, no retailer filter.
- No list totals, budgets, wishlist, tracking, history, alerts, or chat. Those are J05–J12.
- The daily download is a ~2.7 MB file and the first import takes several seconds.
