# Test Case Catalog

[TC-CHAT-002](cases/TC-CHAT-002.md) covers shopping confirmations, retries, and conversation return links.
[TC-CHAT-003](cases/TC-CHAT-003.md) covers shopping tool validation, stale previews, saved alert drafts, and decimal arithmetic.

[TC-CHAT-001](cases/TC-CHAT-001.md) covers DeepSeek conversation and tool calls,
checked prices, pack choices and follow-ups, chat history, and send and reply recovery.

[TC-SHARE-001](cases/TC-SHARE-001.md) checks list creation, switching, renaming, confirmed deletion, scoped budget estimates, export, and imported copies.
[TC-OFFLINE-001](cases/TC-OFFLINE-001.md) checks a downloaded Cart, local edits, and budget calculations with the app stopped.
[TC-DISPLAY-001](cases/TC-DISPLAY-001.md) checks Settings navigation, saved languages, contrast, and keyboard use.
[TC-HISTORY-001](cases/TC-HISTORY-001.md) checks exact-pack charts, date and retailer filters, interactive observation selection, recorded gaps, and retention.
[TC-PERSONAL-001](cases/TC-PERSONAL-001.md) checks export, reset confirmation, rollback, and catalogue preservation.

This index describes maintained checks. Use the [test strategy](strategy.md)
for methods and planned coverage. Detailed steps and limits belong in
`cases/`. Dated results belong in [test history](history.md).

## Latest recorded validation

The 22 November 2026 JaCoCo run passed **153 Java tests** with **81.13% line**
and **62.39% branch** coverage. Three separate targeted budget tests passed.
The 21 November run recorded **22 shopper and 13 milestone Chrome scenarios**
and a passing H2 upgrade and restart check. See the [dated run record](history.md).

## J01–J04 baseline scenarios

| ID | Area | Expected result | Source |
|---|---|---|---|
| [TC-WEB-001](cases/TC-WEB-001.md) | Real pages and forms | Empty state, sample import, multilingual search, new-query/blank-query mode reset, list edits, validation, escaping, and planned screens use real services | `HelloControllerTest` |
| [TC-DATA-001](cases/TC-DATA-001.md) | File storage | Products, quantity, and checked state survive database reopen | `CatalogPersistenceTest` |
| [TC-DATA-002](cases/TC-DATA-002.md) | Source parser | Checked sample labels/offers survive; numeric decimals retain exact precision; unknown price remains null; invalid records are rejected | `CatalogJsonParserTest` |
| [TC-DATA-003](cases/TC-DATA-003.md) | HTTP source client | Successful bytes are returned; failed HTTP status and oversized responses are rejected | `ConsumerCouncilSourceClientTest` |
| [TC-CATALOG-001](cases/TC-CATALOG-001.md) | Import and comparison | Repeated import has no duplicates; three source names find one code; packs stay separate; ties and missing prices are correct; database-write failure rolls back catalogue, metadata, and list | `CatalogServiceIntegrationTest` |
| [TC-LIST-001](cases/TC-LIST-001.md) | Shopping rules and layout | Saved quantities and prices, incomplete totals, and a sticky desktop summary | `ShoppingServiceTest`, Chrome shopper suite |
| [TC-SEARCH-001](cases/TC-SEARCH-001.md) | Search relevance | Ranked Matches/Related, multilingual normalization, and exact source-category filtering | `SearchRelevanceTest` |
| [TC-STRUCTURE-001](cases/TC-STRUCTURE-001.md) | Read/query contract | One Cart read supplies rows, preview totals and budget; products, listings, and offers are fetched together | `ShoppingServiceTest`, `HelloControllerModelTest` |
| [TC-BROWSER-001](cases/TC-BROWSER-001.md) | Browser regression | Native forms, compact Cart edits and done state, Discover budget, responsive navigation, and exact estimates | `market-desk.cjs` |

Case descriptions state expected behaviour; they do not prove a later build passed.

## J05–J08 milestone checks

| ID | Area | Expected result | Source |
|---|---|---|---|
| [TC-TRACK-001](cases/TC-TRACK-001.md) | Wishlist and tracking | Product-wide enrollment on LIVE imports, separate retailer periods, retention, repeat prevention, rollback and file reopen | `TrackingServiceTest`, `TrackingPersistenceRestartTest` |
| [TC-ALERT-001](cases/TC-ALERT-001.md) | Retailer price alerts | Targets, drop evidence, snooze, persisted alert switches, and tracking after rule deletion | `AlertServiceTest`, `TrackingPersistenceRestartTest` |
| [TC-ALERT-002](cases/TC-ALERT-002.md) | Product-wide targets | Qualifying retailers, repeat prevention, rearm, persisted switches, history grouping, and import rollback | `ProductTargetServiceTest`, `AlertTimelineTest`, `TrackingPersistenceRestartTest` |
| [TC-ALERT-003](cases/TC-ALERT-003.md) | Independent and combined Watch rules | Separate thresholds, pending cadence, grouped evidence, concurrent checks, import rollback, and H2 upgrade | `WatchRuleServiceTest` |
| [TC-ALERT-004](cases/TC-ALERT-004.md) | Scheduled and repeating alerts | One-time completion, price repeat limits, Hong Kong calendar repeats, pause, pending rules, and previous H2 constraints | `WatchRuleServiceTest` |
| [TC-BUDGET-001](cases/TC-BUDGET-001.md) | Planned budget | Settings card, selected-retailer totals, Discover and Cart progress, validation, and H2 file reopen | `BudgetServiceTest`, `BudgetPageTest`, `BudgetSettingsPersistenceTest` |
| [TC-REMINDER-001](cases/TC-REMINDER-001.md) | Time alerts | One due occurrence, editable rules, Cart actions, dismissal history, snooze, pause and resume, and H2 upgrade | `ReminderControllerTest`, `ReminderPersistenceRestartTest`, `SecondaryDisplayTest` |
| [TC-MILESTONE-001](cases/TC-MILESTONE-001.md) | Combined browser flow | Wishlist tracking, shared alert editor and history, one-time and calendar rules, legacy rules, budget, and responsive pages | `milestone-check.cjs` |
| [TC-UPGRADE-001](cases/TC-UPGRADE-001.md) | Existing database upgrade | An old baseline's catalogue, selected retailer, quantity and checked state survive the new schema | `persistence-check.cjs` with `BASELINE_JAR` |

The retired prototype cases and their original source links are preserved in
[history](history.md#tc-web-002--retired-prototype-scenes-and-assets).
