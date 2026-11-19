# Test Case Catalog

This index describes maintained checks. Detailed steps and limits belong in
`cases/`; dated results belong in [test history](history.md).

## Latest recorded validation

PR #18 merged J05–J08 on 15 November 2026; issue #17 is closed. The 12 November
2026 review follow-up passed **88 Java tests**, **9 baseline and 11 milestone
Chrome scenarios**, and an old-to-new H2 upgrade check. A fresh full Maven run on
18 November also passed **88 tests** with no failures, errors, or skips. Official
PlantUML exports were rendered and visually reviewed. See the
[dated run record](history.md). The earlier PR #15 results remain there as
historical evidence.

On 18 November 2026, J09–J12 workstream 1 added product-level tracking controls,
a product-wide target, the reminder retailer picker and the unavailable-price
add guard. It passed **90 Java tests**, **9 baseline and 11 milestone Chrome
scenarios**, a file-database restart, and an old-to-new H2 upgrade from the
pre-J05 baseline. These results cover workstream 1 only; the remaining J09–J12
work under issue #21 is not complete.

On 19 November 2026, J11 added the self-contained read-only HTML snapshot. It
passed **141 Java tests**, **9 baseline and 15 milestone Chrome scenarios**, and
the file-database restart check. The milestone run downloads the snapshot, stops
the app, opens the file in an offline browser context, and checks the saved
amounts, saved-on/data-age labels, unknown source date, and absence of scripts,
forms, and live URLs.

On 19 November 2026, J12 completed personal-data view/export/delete, tracked
product history, and the contrast/keyboard review. The full run passed **149
Java tests in 28 suites** with no failures, errors, or skips, **9 baseline and
17 milestone Chrome scenarios**, and the file-database restart check. The
milestone evidence includes the privacy export/delete round trip, tracked
history with a 404 for an unknown product, the history link from `/saved`, a
measured body-text contrast ratio of at least 7:1, a 4 px high-contrast focus
outline, and a bordered status pill. Official PlantUML 1.2026.9 exports were
rendered for `docs/diagrams/j09-j12/`; see the [dated run record](history.md) for
the remaining visual-review limit.

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
| [TC-TRACK-001](cases/TC-TRACK-001.md) | Wishlist and tracking | Product-wide enrollment on LIVE imports, separate retailer periods, retention, repeat prevention, rollback and file reopen | `TrackingServiceTest`, `TrackingPersistenceRestartTest` |
| [TC-ALERT-001](cases/TC-ALERT-001.md) | In-app alerts | Targets, stated drop evidence, quiet hours, snooze, next-day summary catch-up and persisted repeat state | `AlertServiceTest`, `TrackingPersistenceRestartTest` |
| [TC-BUDGET-001](cases/TC-BUDGET-001.md) | Planned budget | Persistent weekly/monthly limit, effective selected-retailer estimate, incomplete-price states, category reconciliation, validation, and H2 file reopen | `BudgetServiceTest`, `BudgetControllerTest`, `BudgetPageTest`, `BudgetSettingsPersistenceTest` |
| [TC-REMINDER-001](cases/TC-REMINDER-001.md) | Replenishment | Persisted due state, clear/snooze timing, manual actions during quiet hours, and unchanged ShoppingService validation | `ReminderControllerTest`, `ReminderPersistenceRestartTest` |
| [TC-MILESTONE-001](cases/TC-MILESTONE-001.md) | Combined browser flow | Save/track/update/alert, budget, reminder actions, native forms, safe timed refresh and responsive pages | `milestone-check.cjs` |
| [TC-UPGRADE-001](cases/TC-UPGRADE-001.md) | Existing database upgrade | An old baseline's catalogue, selected retailer, quantity and checked state survive the new schema | `persistence-check.cjs` with `BASELINE_JAR` |

## J09–J12 language foundation

| ID | Area | Expected result | Source |
|---|---|---|---|
| [TC-LANG-001](cases/TC-LANG-001.md) | Saved language and contrast | English default, saved Traditional/Simplified Chinese on every page, high-contrast marker, matching bundle keys, reviewed product-label fallback, and invalid values rejected without changing the saved choice | `LocalizationTest` |
| [TC-ASK-001](cases/TC-ASK-001.md) | Checked-facts assistant | Deterministic retrieval, chooser/no-match, checked facts, amount validation, safe fallback, and provider timeout/key boundary | `AskServiceTest`, `AskControllerTest`, `HttpAiAnswerClientTest` |
| [TC-SHARE-001](cases/TC-SHARE-001.md) | List transfer | Versioned snapshot export, all-or-nothing import into a separate list, unique names, preview, listed-but-unpriced retailer retention, and size/field rejection | `ListTransferServiceTest`, `ListTransferControllerTest`, `milestone-check.cjs` |
| [TC-OFFLINE-001](cases/TC-OFFLINE-001.md) | Offline saved list | Self-contained read-only HTML snapshot with saved amounts, saved-on/data-age labels, unknown source date, no scripts/forms/live URLs, localization, escaping, and empty/unknown-list states | `OfflineSnapshotServiceTest`, `ListTransferControllerTest`, `milestone-check.cjs` |
| [TC-HISTORY-001](cases/TC-HISTORY-001.md) | Tracked product history | Tracked-only access; real retrieval times; 90-day filtering; explicit 7-day gaps; empty states; link from `/saved` | `ProductHistoryControllerTest`, `ProductHistoryService`, `milestone-check.cjs` |
| [TC-PRIVACY-001](cases/TC-PRIVACY-001.md) | Personal data | Counts, versioned personal-only export, confirmation-gated deletion, catalogue retained, personal tracking choices reset | `PrivacyControllerTest`, `PersonalDataService`, `milestone-check.cjs` |
| [TC-A11Y-001](cases/TC-A11Y-001.md) | Contrast and keyboard | Saved high-contrast theme, measured 7:1 body contrast, visible focus outline, non-colour status border, skip link, responsive layouts | `milestone-check.cjs`, `market-desk.cjs`, `LocalizationTest` |

The retired prototype cases and their original source links are preserved in
[history](history.md#tc-web-002--retired-prototype-scenes-and-assets).
