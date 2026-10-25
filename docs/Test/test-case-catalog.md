# Test Case Catalog

Use this brief index to review the automated test suite. Keep one detailed
document per test case in [`cases/`](./cases/).

The table below is the **retired UI-prototype suite**. The J01–J04 implementation
replaces these six-scene checks with real pages, services, and persistence checks.
The 17 October results remain historical evidence, not results for the new code.

| ID | Area | Test | Expected result | Source at prototype stage |
|---|---|---|---|---|
| TC-WEB-001 (replaced below) | Web MVC | `GET /` rendered the prototype | `200 OK`, HTML, view `market-desk`, expected page title | Historical `HelloControllerTest.java` |
| [TC-WEB-002](./cases/TC-WEB-002.md) | Web MVC | Root page includes prototype scenes and assets | Sample labels, six scenes, local CSS and deferred JavaScript | [`HelloControllerTest.java`](../../src/test/java/com/pricewatch/web/HelloControllerTest.java) |
| [TC-WEB-003](./cases/TC-WEB-003.md) | Web MVC | Root page includes search and demo controls | Visible search label tied to its input, main landmark, navigation, and dialogs | [`HelloControllerTest.java`](../../src/test/java/com/pricewatch/web/HelloControllerTest.java) |

## Prototype review status — historical

- Automated prototype cases: **3**
- Prototype coverage: root-page MVC view and key HTML structure
- These cases do not run JavaScript or prove browser interaction, visual
  layout, keyboard behaviour, real price calculations, or saved data.
- Test execution results must come from the current Maven output. The table
  defines the checks; dated records below state the observed run results.

## Prototype check record — 17 October 2026

- The implementation agent ran the focused `HelloControllerTest` class after
  the screen shell and again after adding demo interactions. Both runs passed
  all three cases.
- Full suite: `.\mvnw.cmd -q test` passed all three tests, with no failures,
  errors, or skipped tests.
- Chrome browser checks: **62 of 62 passed**. All six scenes fit widths of
  375, 768, 1024, and 1440 pixels without horizontal overflow.
- Browser checks covered saved-item and tracking focus, quantity validation,
  dialogs closed with Escape, high contrast, Chinese-name search, tied prices,
  missing listings, reduced motion, and demo reset after reload.
- The browser run reported no console errors, page errors, or external
  requests.
- Limits: the browser checks used Chrome only. Scripted checks are not a full
  accessibility audit. These results do not prove real data import, saved
  storage, AI calls, or notifications; those are outside this UI prototype.

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

These scenario descriptions define expected behaviour. Execution status must be
recorded from the current run, separately from the historical prototype checks.

## Initial baseline check record — 24 October 2026

The implementation agent reported these observed results from the final runs:

| Java suite | Tests | Result |
|---|---:|---|
| `CatalogJsonParserTest` | 7 | Passed |
| `CatalogPersistenceTest` | 1 | Passed |
| `CatalogServiceIntegrationTest` | 5 | Passed |
| `ConsumerCouncilSourceClientTest` | 1 | Passed |
| `ShoppingServiceTest` | 5 | Passed |
| `HelloControllerTest` | 8 | Passed |
| **Total** | **27** | **0 failures, 0 errors, 0 skipped** |

- Final command: `.\mvnw.cmd -q test`, exit status 0. Mockito's dynamic-attach
  warning was non-failing output.
- Baseline Chrome checks: **44/44 passed**. An earlier 43/44 report came from a
  test assertion that assumed a redirect had no session-ID suffix. The actual
  no-JavaScript form saved the expected quantity; the corrected assertion and
  fresh-database rerun passed.
- Separate live-data Chrome checks: **9/9 passed**. The 24 October response was
  accepted with **2,735 products, 7,812 listings, and 2,618 offers**. Its numeric
  product code `173520` exposed a source-shape difference from the saved excerpt.
  After the explicit integral-number identifier rule and regression checks were
  added, the import succeeded. The full live file is not committed.
- A real process stop/start against the same file database preserved 2,735
  accepted products and the same retrieval timestamp. The saved list item still
  had quantity **3**, checked state **true**, and line total **HK$22.50**.
- All three UML sources rendered locally as SVG and PNG through documd 5.3.2.
  Images were visually inspected. The documented regeneration script completed
  successfully, and local Markdown link targets were checked.

J01–J04 meet their first-milestone completion checks. Later jobs remain planned.
Browser checks used Chrome only; they are not a full accessibility audit. These
results do not prove other browsers, long-term provider/code stability, or any
tracking, notification, AI, sharing, budget, or offline-phone feature.

Final request-guard checks: three additional browser checks passed after adding
`LocalRequestGuard`. Cross-origin browser writes return 403. Normal same-origin
forms still add and edit list items with JavaScript disabled. The guard also
rejects non-loopback hostnames. Non-browser clients without Origin/Fetch Metadata
headers can still use the local app; this is not an account or authentication system.

## Retailer, relevance, and navigation refinement checks

The earlier 27-test record is historical baseline evidence. The current
refinements add saved explicit/automatic retailer selection, separate ranked
Matches/Related search groups, category filtering, grouped desktop navigation,
and Discover/List/Data/More phone navigation.

Final refinement validation on 24 October 2026:

| Suite | Tests | Result |
|---|---:|---|
| `CatalogJsonParserTest` | 7 | Passed |
| `CatalogPersistenceTest` | 1 | Passed |
| `CatalogServiceIntegrationTest` | 5 | Passed |
| `ConsumerCouncilSourceClientTest` | 1 | Passed |
| `SearchRelevanceTest` | 4 | Passed |
| `ShoppingServiceTest` | 10 | Passed |
| `HelloControllerTest` | 10 | Passed |
| **Total** | **38** | **0 failures, 0 errors, 0 skipped** |

- Full command: `.\mvnw.cmd -q test`, exit 0. Focused search, shopping,
  persistence, and web checks also ran after their coherent changes.
- Independent code review found a mixed-token classification issue. The parent
  corrected it and added a regression: a direct name match plus a broad-category
  token stays in Matches. Broad-category-only records remain Related.
- Chrome: **41/41** checks passed against the final build. They cover required
  retailer choice, exact quantity previews, saved choices and deliberate edits,
  conflicting-choice rejection, native forms without JavaScript, real noodle
  relevance, related results, source-category filtering, active Data navigation,
  mobile More navigation, and layouts at 375/768/1024/1440 px. No page/console
  errors were reported. Screenshots were visually inspected.
- A real process restart preserved JASONS, quantity 2, and checked state true.
  Database-reopen and disappeared-selected-retailer cases are also automated.
- The live response used for the query had 80 direct matches and 346 broad-only
  related matches for `noodle`. These are observations of that feed, not fixed
  future counts.
- Updated class and sequence diagrams rendered locally to SVG/PNG and were
  visually inspected. The parent checked the source interfaces against code.

Limits remain: offers are displayed but not calculated; no typo correction,
semantic translation, or cross-code product merging was added. Chrome checks
are not a complete accessibility audit. Runtime preview uses an isolated
verification database and does not alter the user's running port-8080 process.


## Code structure cleanup — 24 October 2026

The later cleanup keeps the existing routes and behavior. Pages now use separate
Thymeleaf fragments and shared layout/navigation. The list page reads its rows
once and derives its summary from the same rows. Mapping those rows uses the
already-fetched products, listings, and offers.

- `.\mvnw.cmd -q test`: **42 tests passed**, with no failures, errors, or skips.
  This adds one shopping-query regression and three controller-model regressions
  to the earlier 38-test record.
- [TC-STRUCTURE-001](cases/TC-STRUCTURE-001.md) verifies one database query for a
  populated list and no unused list reads on Data, Planned, or list mutations.
- `.\mvnw.cmd -q package`: passed before browser execution. After the small CSS
  correction, packaging and the complete Java test suite passed again.
- [TC-BROWSER-001](cases/TC-BROWSER-001.md): `npm run test:browser` passed all
  **8 scenarios** using Chrome. It starts an isolated app with an in-memory
  database and local synthetic feed. It does not test the current live provider.
- The browser suite covers native forms without JavaScript, chosen-retailer
  persistence and edits, missing selected prices, Unicode/multilingual search,
  related/category results, exact decimal boundaries, and keyboard navigation.
  Responsive checks cover five routes at 375, 768, 1024, and 1440 px.
- Screenshot review exposed clipped large supported prices. A new regression
  failed before the CSS fix and passed after it. Full decimal values now wrap
  in cards, comparison cells, list totals, and the selection estimate.
- Shared templates rendered through the existing controller integration suite.
  UML class/sequence sources and SVG/PNG exports were updated and inspected.

Browser checks were run with Chrome only. Other browsers and full accessibility
conformance are not verified by this cleanup. The app still needs no Node runtime;
Node and Playwright are optional development tools for the saved browser suite.


## Database rollback review follow-up — 24 October 2026

- Corrected the implementation guide's search route from `tab` to `related`.
  `related=true` selects Related results; omission or `related=false` selects Matches.
- Added the direct database-write recovery case in
  [TC-CATALOG-001](cases/TC-CATALOG-001.md). A real temporary H2 constraint rejects
  an insert after catalogue replacement has begun. Fresh reads confirm that the
  previous catalogue, accepted-import metadata, selected retailer, quantity, and
  checked state remain intact. The new replacement product does not survive.
- Focused command: `.\mvnw.cmd '-Dtest=CatalogServiceIntegrationTest' test`.
  All **6 integration tests passed**.
- Full command: `.\mvnw.cmd -q test`. All **43 tests passed**, with no failures,
  errors, or skips. The logged test constraint violation is deliberate.

This follow-up changes tests and documentation. The earlier browser check record
remains separate evidence; browser checks and live issue #13 compliance were not
rechecked for this follow-up.


## Store-choice cards — 25 October 2026

A browser regression reproduced a missed selection when clicking the original
Lowest listed badge outside the radio label. The comparison now uses native
radio-backed cards with visually hidden circles. Card names, prices, badges,
offer copy, and padding select the store. The chosen card has a coloured border,
a tinted background, and a Selected marker. Chinese offer disclosures remain
separate interactive controls. Missing-price cards show No current price and
remain disabled for a new explicit choice.

- Focused `HelloControllerTest`: 10 tests passed.
- Full `.\mvnw.cmd -q test`: 43 tests passed, with no failures, errors, or skips.
- `npm run test:browser` with Chrome: all 9 scenarios passed. Added checks cover
  card targets at 375/1440 px, price and availability descriptions for assistive
  technology, selected markers, arrow-key selection, separate disclosures,
  unavailable choices, and native selection without JavaScript.
- Packaging passed. Desktop and mobile card screenshots were inspected under
  ignored `target/browser-checks/store-choice-{1440,375}.png`.

These checks do not establish conformance across all browsers or a complete
accessibility audit. The earlier 24 October records remain historical evidence.


## Official PlantUML remake — 25 October 2026

The class, sequence, and use-case diagrams were remade against the current Java
code and implementation guide. This changes documentation and diagram tooling;
it does not change the app's catalogue or shopping behavior.

- Final SVG and PNG files were exported with the official PlantUML 1.2026.9 Java
  JAR. All three sources passed Java syntax checking and the installed official
  PlantUML MCP syntax checker, with no MCP warnings.
- Class relationships were checked against JPA mappings, including five product
  embedded labels, offer text, cascade/orphan ownership, retained inactive
  products, a retailer code without a listing foreign key, and independent
  singleton import metadata. Services are explained in the sequence view.
- Sequence review corrected search ranking to occur in CatalogService and kept
  automatic retailer-price resolution on list reads. Parsing precedes replacement;
  commit/rollback are within the replacement-service transaction, while UI error
  handling is outside it. Download failure does not proceed to parsing.
- All three replacements were rendered and visually compared with the previous
  exports before replacement. Editable sources, SVG, and PNG are retained.
- The new Java export script was checked with valid sources, a path containing
  spaces, and invalid source. Invalid syntax stopped the run without changing an
  existing SVG. Script documentation distinguishes official Java exports, MCP
  checks, and historical documd-compatible exports.
- Full `.\mvnw.cmd -q test`: 43 tests passed, with no failures, errors, or skips.
  Browser checks were not rerun for this UML-only change; the nine-scenario
  retailer-card run above remains the relevant browser evidence.

The previous local diagram set was kept in ignored
`target/uml-remake/previous/` for comparison, then removed during cleanup on
25 October 2026. This official renderer verification does not confirm the
unspecified course tutorial method.


## Separate data and application class views — 25 October 2026

The persisted data model remains `class.puml`, now arranged across two landscape
rows. Product fields explicitly show brand, name, and category1/2/3 as embedded
LocalizedLabel slots. Language fields and JSON keys are explained. The database
allows nullable embedded columns (`0..5` product embeddings and `0..1` offer text,
not collections); product imports require all five labels in all three nonblank
languages. Database mappings/constraints and parser/service rules have separate
legend columns. Quantity limits and one-list/metadata-ID policies are not labelled
as SQL CHECK constraints.

The new `application-class.puml` is a separate landscape application view. Its
controller, filter, services, parser/import writer/source client, six repository
interfaces, key operations, and framework collaborator types were checked against
current code. A code-derived check verified all 16 displayed constructor
collaborator dependencies. The old architecture SVG was used only as a layout
reference; its implementation content was not copied.

- Official PlantUML 1.2026.9 syntax checks and SVG/PNG exports passed for all four
  diagrams. The default render script now includes the application class view;
  the two-class-view subset command also passed.
- The PlantUML MCP accepted all four sources with no warnings.
- Both class exports were visually inspected. Data model: 1219 x 675 px;
  application classes: 1440 x 1102 px. Final exports matched reviewed candidates.
- Full `.\mvnw.cmd -q test`: 43 passed; zero failures, errors, or skips.
- Documentation links and `git diff --check` passed. Browser checks were not
  rerun for this UML/documentation-only update.

The pre-update set and intermediate review files under ignored
`target/uml-layout-review/` were removed during cleanup on 25 October 2026.
The accepted editable `.puml`, SVG, and PNG exports are kept in the UML folder.

## PR #15 review regressions — 25 October 2026

Both automated review findings were reproduced before their fixes. Numeric JSON
`100000000000000.0001` lost its final digit, excess fractional digits could be
accepted, and stale Related mode hid catalogue products after clearing a query.

- TC-DATA-002 now checks exact numeric-token amounts and rejection of unsupported
  fractional precision. The parser uses a decimal-preserving reader without
  changing the shared ObjectMapper configuration.
- TC-WEB-001 checks that new search forms omit Related mode and blank queries
  ignore stale `related=true` parameters. Explicit Related links remain supported.
- TC-BROWSER-001 exercises changing and clearing queries from Related, plus a
  direct stale blank Related URL, within its existing search scenario.
- Focused parser/controller checks passed all 21 tests.
- Full Maven `package` passed all 47 tests with no failures, errors, or skips.
  The existing Byte Buddy agent was supplied through command-line `argLine`;
  local HTTP tests ran outside the sandbox to permit loopback networking.
- `npm run test:browser` with Chrome passed all 9 scenarios against the rebuilt
  JAR and an isolated database/feed.
