# TC-JAVA-COVERAGE-001 · Java unit and integration coverage

Use JDK 21, the Maven wrapper, and the JaCoCo command in the
[test strategy](../strategy.md). Use a clean coverage build with no class
exclusions. The tests use local HTTP fixtures and isolated H2 databases.

| Area | Cases | JUnit source |
|---|---|---|
| Catalogue validation | Missing, malformed, duplicated, and oversized inputs, distinct product and retailer identities, incomplete offer translations | [Catalogue parser](../../../src/test/java/com/pricewatch/catalog/CatalogJsonParserTest.java) |
| Feed and locale | Chunked oversize, disconnected transport, unsupported URI, and label fallback | [Feed client](../../../src/test/java/com/pricewatch/catalog/ConsumerCouncilSourceClientTest.java) · [Localized text](../../../src/test/java/com/pricewatch/catalog/LocalizedTextTest.java) |
| Tracking | Inactive or invalid targets, ending individual and product-wide tracking, and repeatable demo history with gaps | [Tracking with H2](../../../src/test/java/com/pricewatch/catalog/TrackingServiceTest.java) |
| Cart and budget | Import format, record count and size boundaries, malformed retailer and quantities, rejected names and inactive catalogue references, and budget category fallback | [List transfer validation](../../../src/test/java/com/pricewatch/shopping/ListTransferValidationTest.java) · [Transfer with H2](../../../src/test/java/com/pricewatch/shopping/ShoppingListTransferTest.java) · [Budget](../../../src/test/java/com/pricewatch/budget/BudgetServiceTest.java) |
| Ask | Tool validation, exact product matching, named Cart and Wishlist operations, alert drafts and dispatch, decimal bounds, malformed provider responses, and session-bound return links | [Shopping tools](../../../src/test/java/com/pricewatch/web/AskShoppingToolsTest.java) · [Catalogue tools](../../../src/test/java/com/pricewatch/web/AskCatalogueToolsTest.java) · [Provider HTTP fixture](../../../src/test/java/com/pricewatch/web/DeepSeekChatClientTest.java) · [MockMvc](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java) |
| Reminders | Upgrade rollback and idempotence, schedule and snooze, history and Cart defaults | [Reminder upgrade integration](../../../src/test/java/com/pricewatch/notifications/persistence/LegacyReminderMigrationTest.java) |
| Alert rules | Calendar edits after a due occurrence, Wishlist requirements for price rules, live processing of paused and time-only rules, and page actions scoped to the correct product | [Watch rules with H2](../../../src/test/java/com/pricewatch/notifications/WatchRuleServiceTest.java) · [Alert Center with MockMvc and H2](../../../src/test/java/com/pricewatch/web/AlertCenterControllerTest.java) |
| Request and message handling | Localhost and same-origin requests, rejection of private IPv4 hosts under the default, and translated validation errors | [Request guard](../../../src/test/java/com/pricewatch/web/LocalRequestGuardTest.java) · [Validation messages](../../../src/test/java/com/pricewatch/web/MilestoneMessagesTest.java) |

Confirm each test asserts an observable result, such as the rejected input,
saved state, rendered response, recovery behaviour, or decimal amount.
Compare line and branch counts against the [dated baseline](../history.md).
Unit and integration results are part of the same Maven test run. Inspect any
remaining source decision before treating it as reachable through the app.
