# Test Case Catalog

This index describes maintained checks. Detailed steps and limits belong in
`cases/`; dated results belong in [test history](history.md).

## Latest recorded validation

The 25 October 2026 PR #15 review-fix run passed **47 Java tests**, packaging,
and **9 Chrome scenarios**. See the [run record](history.md#pr-15-review-regressions--25-october-2026).
These are recorded results, not a new run for the 29 October documentation cleanup.
J05–J08 checks must be added with their implementation.

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
