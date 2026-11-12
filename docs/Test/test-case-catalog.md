# Test Case Catalog

This index describes maintained checks. Detailed steps and limits belong in
`cases/`; dated results belong in [test history](history.md).

## Latest recorded validation

The 12 November 2026 J05–J08 implementation run passed **76 Java tests** (see the
[run record](history.md#j05j08-implementation--12-november-2026)). The most recent
browser run remains the 25 October PR #15 run: **9 Chrome scenarios**. J05–J08 are
not closed until the combined acceptance flow and the integrated browser check are
recorded.

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
The retired prototype cases and their original source links are preserved in
[history](history.md#tc-web-002--retired-prototype-scenes-and-assets).

## J05–J08 scenarios

| ID | Area | Expected result | Source |
|---|---|---|---|
| [TC-TRACK-001](cases/TC-TRACK-001.md) | Wishlist and tracking | Save ≠ track; observations per priced pair per import; failed import adds none; history survives replacement; untrack stops collection | `TrackingServiceTest` |
| [TC-BUDGET-001](cases/TC-BUDGET-001.md) | Planned budget | Known subtotal vs limit; category totals reconcile; unknown prices incomplete; JASONS 12.50 regression | `BudgetServiceTest` |
| [TC-ALERT-001](cases/TC-ALERT-001.md) | In-app alerts | Target at/below; drop vs distinct prior; no duplicate events; quiet hours, snooze, summary | `AlertServiceTest` |
| [TC-REMINDER-001](cases/TC-REMINDER-001.md) | Reminders | Interval/snooze due dates; add-to-list reuses list rules; clear deactivates; survives restart | `ReminderServiceTest` |
