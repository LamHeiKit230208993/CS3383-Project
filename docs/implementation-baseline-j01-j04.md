# Market Desk: J01–J04 implementation

Updated: 25 October 2026. This is the independent implementation for issue #13. It uses the current branch and does not merge another team member's version.

## What the first baseline does

The working flow is **load prices → search → compare one exact product → add a quantity → edit My list → restart and read the same list**.

The app uses Spring Boot, Thymeleaf, plain HTML/CSS, Spring Data JPA, and a local H2 file database. It does not use React or a separate frontend build. The browser submits forms to Java controllers. Application services own validation, comparison, and totals. Repositories save the records.

This milestone does not add accounts, cloud saving, AI, tracking history, notifications, budgets, list transfer, or offline phone storage. Saved, Updates, and Ask are marked planned. Their earlier demo controls are not working baseline features.

## Records and rules

| Record | Meaning |
|---|---|
| Product | One source code, with names, brands, and three category levels in English, Traditional Chinese, and Simplified Chinese |
| Retailer listing | One product/retailer pair, with a nullable decimal price |
| Offer | Original retailer-linked text in all available source languages |
| Accepted import | Origin, source URL, retrieval time, nullable source date, checksum, and counts |
| Shopping list item | Product, whole quantity from 1–999, checked state, and nullable selected retailer in the single My list |

Money uses `BigDecimal`. Numeric JSON prices are read as decimal values before validation, so they follow the same exact scale/precision rules as string prices without a double conversion. An absent price is unknown, never zero. Offer text is not a calculated per-pack price. Product names retain source pack text. Similar labels with different source codes stay separate.

The 24 October 2026 live response contained 2,735 products and one numeric product code, `173520`. The importer accepts a nonnegative integral JSON number as its exact canonical decimal identifier string. Source string codes remain strings without numeric rewriting. Negative and fractional numeric codes are rejected. Duplicate checks run after this normalization, so numeric `173520` and string `"173520"` cannot conceal conflicting records. The full live response is not committed. The unchanged 17 October sample and its source-inspection notes remain separate evidence.

Search reads stored labels across all three languages, plus brands, categories, source codes, and retailer codes. It normalizes Unicode with NFKC, lowercases text, and collapses whitespace. Every query token must match. Exact code/name and product-name matches come first; ordinary field relevance ranks name above brand, category level 3, then category level 2. Only products whose query tokens match exclusively through the broad level-1 category go into the separate Related group. A name match combined with a broad-category word stays in Matches. Discover opens on Matches by default. A category filter uses exact normalized English level-2 source labels. Search does not correct spelling, translate queries, or merge source codes. Comparison keeps every tied lowest retailer.

Adding the same product with the same retailer choice increases its quantity. Adding it with a different choice is rejected with a direction to change the retailer in My list. There remains one item per product, not separate rows for every retailer. Invalid quantities, including a combined quantity above 999, produce an error rather than clipping the number. Updating checked state does not record an actual purchase.

Each item can use **Automatic lowest** or an explicit retailer. A null `selectedRetailerCode` means automatic; it uses a current lowest known listing. An explicit choice uses only that retailer's price. The selection is saved across restart. If that retailer later loses its price or listing, the choice stays saved and the amount becomes unknown. The app does not silently switch retailers. New or changed explicit choices require a current known price; an existing unavailable choice can remain while quantity/checked state changes.

List totals multiply those effective unit prices by quantities. They are not the cost of buying the whole basket in one store. If an item has no known price, the screen shows a **known subtotal** and the number of unavailable items. Products missing from a later catalogue remain available to their saved list, but have no current price. Offers remain original text and are labelled **not calculated**; quantity does not activate multi-buy arithmetic.

## Imports and dates

There are two manual actions:

- **Load checked sample** reads the unchanged two-product excerpt checked on 17 October 2026. It is historical sample data, not the full catalogue or current prices.
- **Refresh live prices** downloads the fixed [Consumer Council endpoint](https://online-price-watch.consumer.org.hk/opw/opendata/pricewatch.json). The endpoint is not a user-entered URL.

If any catalogue is already accepted, the sample action requires explicit server-side confirmation before replacement. This prevents an ordinary sample-form submission from silently replacing the full live catalogue with two historical products.

Startup does not fetch data. A saved catalogue remains readable without an internet connection while the local Java app runs. This does not implement reopening a saved page on an offline phone.

The importer validates the full response before replacement. Empty input, malformed records, conflicting duplicate codes/listings, and invalid or negative prices are rejected. Additional source fields are ignored. Repeated imports replace accepted listings without duplicates. A failed download, rejected response, or failed database write keeps the last accepted catalogue and the shopping list.

The source client limits responses to 10 MiB. Connection and read timeouts are 30 seconds each. This is not a measured end-to-end response-time promise. Stored money allows four fractional decimal places; values that need rounding to fit are rejected. Source labels and codes are checked against database field limits before replacement.

Retrieval time means when this app obtained the response. It is not the source price date. The checked structure has no source-date field, so the app leaves that date unknown. The accepted import is catalogue metadata, not a historical price series.

## Web interface

| Request | Purpose |
|---|---|
| `GET /?q=…&category=…&related=true&page=…` | Ranked Matches or Related results, source category filter, and pagination |
| `GET /products/{code}` | Exact product and retailer comparison |
| `GET /list` | Persisted My list and known subtotal |
| `GET /data` | Accepted source details and manual import actions |
| `GET /planned/{feature}` | Clear later-work placeholder |
| `POST /data/sample` | Load the checked historical excerpt |
| `POST /data/refresh` | Fetch and accept live data if valid |
| `POST /list/items` | Add product and quantity |
| `POST /list/items/{code}/update` | Update quantity and checked state |
| `POST /list/items/{code}/remove` | Remove a list item |

Omit `related` or set `related=false` for Matches. Use `related=true` for Related results.
Submitting a new search or category filter returns to Matches. Pagination keeps
the selected result group. Blank queries ignore `related=true` and show the
accepted catalogue, subject to the selected category filter.

Successful changes redirect to a normal page. There is no separate JSON API. Source labels are multilingual, but the app interface remains English. Full R46 translations and the remaining J12 work are not complete.

Comparison uses clickable retailer cards. The selected card has a coloured border,
a tinted background, and a visible Selected marker. Native radio controls are
visually hidden but retain keyboard, screen-reader, and no-JavaScript form
behaviour. A card's price, name, offer copy, and lowest-price badge share its
selection target. Chinese offer details are a separate disclosure and do not
change the selected retailer. Missing-price cards show No current price and
remain unavailable for a new explicit choice.

Desktop navigation groups working shopping pages under **Shopping**, planned alerts/chat under **Assistance**, and **Price data & updates** under **App**. Saved remains labelled planned. Data uses the same selected-row treatment as the other pages. Phone navigation uses Discover, List, Data, and More; More holds the planned features.

### Code structure

`market-desk.html` is the shared shell. Each screen lives under `templates/pages/`.
Shared navigation, source status, and layout elements live under
`templates/fragments/`. This uses standard Thymeleaf fragments, without another
layout library. CSS, JavaScript hooks, and routes remain shared.

The list repository fetches items, products, listings, and offers together.
`ProductView.from(product)` maps that loaded data without another database lookup.
The list page reads the items once and uses `Summary.from(items)` for its totals,
so the rows and summary use the same data. Other pages read a summary only when
they display it. The optional browser check command is in the setup guide.

### Java interfaces

`CatalogService.searchResults(query, category)` returns `SearchResults(matches, related, categories)`. `search(query)` remains compatible and combines ranked Matches followed by Related. The service also exposes `find(code)`, `status()`, `importSample()`, and `refreshLive()`. `ProductView` carries source identity, localized labels, active status, current listings, the lowest known price, and all tied lowest listings. `ListingView` keeps a nullable price and original offers. `ImportStatus` is absent until the first accepted import.

`ShoppingService` exposes `items()`, `summary()`, `add(code, quantity, retailerCode)`, `update(code, quantity, checked, retailerCode)`, and `remove(code)`. The two-argument add defaults to automatic. The three-argument update preserves an existing selection. Each `ItemView` combines the saved item with its effective price and `selectedRetailerCode`. `Summary` contains `knownSubtotal`, `unknownItemCount`, and `totalQuantity`. Checking an item does not remove it from the planned total.

`CatalogJsonParser` produces validated import records before persistence. `CatalogReplacementService` replaces the latest catalogue and singleton accepted-import metadata in one transaction. Products absent from a replacement are inactive; their list references remain. Repositories are the persistence boundary. Normal controllers do not fetch source JSON or calculate money themselves.

## Diagrams and verification

The [UML sources and exports](diagrams/baseline/README.md) describe this milestone in four views: use case, landscape persisted data model, separate landscape application classes, and main-flow sequence. Sources remain editable. Final SVG/PNG exports use the official PlantUML 1.2026.9 Java JAR and one Print palette. The UML README distinguishes the Java renderer, optional MCP checks, and previous documd-compatible exports.

See the [setup guide](SETUP.md) for run commands and database configuration. See the [test catalogue](Test/test-case-catalog.md) for automated scenarios and dated observed results. A source file, diagram, or successful compile alone does not prove J01–J04 complete.

J01–J04 implementation checks passed on 24 October 2026. The latest full Maven run passed 43 tests, including the direct database-write rollback case. The 25 October retailer-card run passed all 9 saved Chrome scenarios. The later UML-only remake did not rerun browser checks. The dated test catalogue separates these runs from earlier baseline, refinement, live-feed, and process-restart evidence. These changes do not complete the later J05–J12 jobs.

Issue #13 also requires the method shown in the course tutorial. The data model, application classes, and sequence diagrams cover the implementation and requested flow, but the issue does not identify that method. Confirm the tutorial-specific method with the team before claiming that this course requirement is verified.

The local web request guard permits `localhost` and loopback hostnames. It rejects
browser writes with a different Origin or cross-site Fetch Metadata. This blocks
unrelated websites from submitting forms to the running app. It does not add user
accounts or make the local app suitable for public network hosting.
