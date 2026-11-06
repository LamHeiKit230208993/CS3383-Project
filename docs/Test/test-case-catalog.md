# Test Case Catalog

This index describes maintained checks. Detailed steps and limits belong in
`cases/`; dated results belong in [test history](history.md).

## Latest recorded validation

The 6 November 2026 J05–J08 run passed **86 Java tests**, packaging,
**9 baseline and 11 milestone Chrome scenarios**, and an old-to-new H2 upgrade
check. Official PlantUML exports were rendered and visually reviewed.
See the [dated run record](history.md). The earlier PR #15 results remain there
as historical evidence. These local results do not close issue #17 or imply merge.

## J01–J04 baseline scenarios

| ID | Area | Expected result | Source |
|---|---|---|---|
| [TC-WEB-001](cases/TC-WEB-001.md) | Real pages and forms | Empty state, sample import, multilingual search, new-query/blank-query mode reset, list edits, validation, escaping, and planned screens use real services | `HelloControllerTest` |
| [TC-DATA-001](cases/TC-DATA-001.md) | File storage | Products, quantity, and checked state survive database reopen | `CatalogPersistenceTest` |
| [TC-DATA-002](cases/TC-DATA-002.md) | Source parser | Checked sample labels/offers survive; numeric decimals retain exact precision; unknown price remains null; invalid records are rejected | `CatalogJsonParserTest` |
| [TC-DATA-003](cases/TC-DATA-003.md) | HTTP source client | Successful bytes are returned; failed HTTP status and oversized responses are rejected | `ConsumerCouncilSourceClientTest` |
| [TC-CATALOG-001](cases/TC-CATALOG-001.md) | Import and comparison | Repeated import has no duplicates; three source names find one code; packs stay separate; ties and missing prices are correct; database-write failure rolls back catalogue, metadata, and list | `CatalogServiceIntegrationTest` |
| [TC-LIST-001](cases/TC-LIST-001.md) | Shopping rules | Quantity and checked state persist; incomplete totals and vanished products stay clear; failed import preserves list | `ShoppingServiceTest` |
| [TC-SEARCH-001](cases/TC-SEARCH-001.md) | Search relevance | Ranked Matches/Related, multilingual normalization, and exact source-category filtering | `SearchRelevanceTest` |
| [TC-STRUCTURE-001](cases/TC-STRUCTURE-001.md) | Read/query contract | One item read supplies rows and summary; products, listings, and offers are fetched together | `ShoppingServiceTest`, `HelloControllerModelTest` |
| [TC-BROWSER-001](cases/TC-BROWSER-001.md) | Browser regression | Native forms, retailer cards, search-mode reset, responsive navigation, and exact estimates | `market-desk.cjs` |

Case descriptions state expected behaviour; they do not prove a later build passed.

## J05–J08 milestone checks

| ID | Area | Expected result | Source |
|---|---|---|---|
| [TC-TRACK-001](cases/TC-TRACK-001.md) | Wishlist and tracking | Explicit product/retailer periods, LIVE-only observations, gaps, retention, repeat prevention, rollback and file reopen | `TrackingServiceTest`, `TrackingPersistenceRestartTest` |
| [TC-ALERT-001](cases/TC-ALERT-001.md) | In-app alerts | Targets, stated drop evidence, quiet hours, snooze, summaries and persisted repeat state | `AlertServiceTest`, `TrackingPersistenceRestartTest` |
| [TC-BUDGET-001](cases/TC-BUDGET-001.md) | Planned budget | Persistent weekly/monthly limit, effective selected-retailer estimate, incomplete-price states, category reconciliation, validation, and H2 file reopen | `BudgetServiceTest`, `BudgetControllerTest`, `BudgetPageTest`, `BudgetSettingsPersistenceTest` |
| [TC-REMINDER-001](cases/TC-REMINDER-001.md) | Replenishment | Persisted due state, clear/snooze timing, manual actions during quiet hours, and unchanged ShoppingService validation | `ReminderControllerTest`, `ReminderPersistenceRestartTest` |
| [TC-MILESTONE-001](cases/TC-MILESTONE-001.md) | Combined browser flow | Save/track/update/alert, budget, reminder actions, native forms, safe timed refresh and responsive pages | `milestone-check.cjs` |
| [TC-UPGRADE-001](cases/TC-UPGRADE-001.md) | Existing database upgrade | An old baseline's catalogue, selected retailer, quantity and checked state survive the new schema | `persistence-check.cjs` with `BASELINE_JAR` |

The retired prototype cases and their original source links are preserved in
[history](history.md#tc-web-002--retired-prototype-scenes-and-assets).
