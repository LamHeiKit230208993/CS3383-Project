# Test History

Dated observations are preserved here. They describe the code and environment
used for each run, not current coverage. Use the [current catalogue](test-case-catalog.md)
for maintained cases.

## Demo history review retest — 23 November 2026

Two regressions from PR #24 failed before the fix and passed after it.
Wishlist now shows all three demo products immediately, with their history
links. The AEON milk series runs from 21 days ago to one day ago, and the
latest stored price and timestamp match the last observation.

All 339 Maven tests passed. JaCoCo measured 3,705/3,939 Java lines (94.06%)
and 1,769/2,097 branch outcomes (84.36%). See the
[demo history case](cases/TC-DATA-005.md) for the inputs and expected results.
Chrome passed 22 shopper and 13 milestone scenarios. The H2 file restart
and J01–J08 upgrade checks also passed on this build.

## Full Java coverage run — 22 November 2026

JaCoCo 0.8.14 measured all 168 application classes in a clean build. The
baseline below uses the previous tests. The new run includes additional JUnit
unit and integration tests and removes three unreachable checks without
changing application behaviour. The report uses no class exclusions.

| Measure | Previous run | Full run |
|---|---:|---:|
| Maven tests passed | 164 | 339 |
| Java source lines | 3,236/3,943 (82.07%) | 3,703/3,938 (94.03%) |
| Java branch outcomes | 1,336/2,107 (63.41%) | 1,769/2,097 (84.36%) |
| Java instructions | — | 21,997/23,810 (92.39%) |
| Java methods | — | 1,003/1,079 (92.96%) |
| Java classes with executed code | — | 159/168 (94.64%) |

The [Java coverage case](cases/TC-JAVA-COVERAGE-001.md) lists the test
inputs and sources. The 100% line and branch target remains open. The
largest measured branch gap is Ask shopping tools (128 missed outcomes
including its nested turn class). The Ask and main web controllers and
the watch-rule service also have untested outcomes.

Chrome passed 22 shopper and 13 milestone scenarios against the updated
application. The H2 checks passed for file restart and for upgrade from the
J01–J08 database, including retailer choices and tracked prices.

## Coverage follow up — 22 November 2026

The comparison uses the same production Java source before and after the
new tests. The baseline is the merged branch at `4a61bf1`, which includes the
demo-history update. The earlier PR #24 baseline remains below.

| Measure | Merged baseline | Follow-up run |
|---|---:|---:|
| Maven cases passed | 154 | 164 |
| Java source lines | 3,214/3,943 (81.51%) | 3,236/3,943 (82.07%) |
| Java branch outcomes | 1,320/2,107 (62.65%) | 1,336/2,107 (63.41%) |
| Web package branch outcomes | 557/1,126 (49.47%) | 571/1,126 (50.71%) |

The follow-up verified stale and failed Ask confirmations, catalogue refresh
failure and consent, quantity bounds, and selected budget and Ask decisions.
The [test strategy](strategy.md#coverage-follow-up) links the case records
and identifies the scope of the independence evidence.

Chrome 157 passed 22 shopper and 13 milestone scenarios. The H2 file restart
and upgrade checks also passed. The shopper run caught a mobile price history
layout issue introduced with the Source column. The updated 375 px check
verified the price stays on one line and the source remains visible.

| Browser script | Executed V8 UTF-16 units | Share |
|---|---:|---:|
| Cart | 2,947/3,250 | 90.68% |
| Ask | 8,140/8,838 | 92.10% |
| Offline checklist inline script | 3,525/3,572 | 98.68% |

Two Node unit tests passed for JavaScript range aggregation and required
script validation. The [Cart and Settings walkthrough](cases/TC-USABILITY-001.md)
records agent-operated browser observations and phone-width retests.

## Java coverage baseline — 22 November 2026

- JaCoCo 0.8.14 measured the Java suite against PR #24 head `741d3fe` in an
  isolated build. All 153 tests passed. Coverage was 81.13% for lines
  (3,135/3,864), 62.39% for branches (1,297/2,079), 77.14% for instructions
  (17,858/23,149), 84.72% for methods (904/1,067), and 90.36% for classes
  (150/166). The web package covered 551 of 1,118 branch outcomes (49.28%).
- Three separate JUnit cases passed for the MC/DC independence pairs of the
  first Budget progress guard. See the [case values](cases/TC-BUDGET-001.md#targeted-decision-coverage).
  This focused run is separate from the coverage baseline.
- The existing Chrome and H2 evidence is recorded below. See the
  [test strategy](strategy.md) for scope and follow-up coverage.

## J09–J12 PR evidence — 21 November 2026

- Official PlantUML 1.2026.9 checked both J09–J12 sequences and exported
  SVG and PNG files. The PNGs were visually reviewed.
- The preview set combines five supplied desktop captures with three
  mobile captures from the controlled Chrome workflows.

## Settings sections and back controls — 21 November 2026

- 153 Maven tests, 22 Chrome shopper scenarios, and 13 Chrome milestone
  scenarios passed. The database upgrade and restart checks passed.
- All four desktop Settings links reached the intended scroll offset and
  respected reduced motion. At 375 px, each submenu returned through its
  arrow. The display switch worked with pointer and keyboard input.
- Back arrows revealed the destination name on hover and focus. Returning
  from a comparison preserved the originating Cart or Wishlist.

## Interactive offline Cart and pinned list summary — 21 November 2026

- 152 Maven tests, 21 Chrome shopper scenarios, and 13 Chrome milestone
  scenarios passed. The file database upgrade and restart checks passed.
- The standalone HTML recalculated quantities, retailer prices, and budget
  with the server stopped and Chrome offline. It made no HTTP requests.
  The desktop Cart summary stayed in view while scrolling and kept its
  expanded controls reachable. The mobile summary followed the item list.

## Named carts and Ask shopping tools — 21 November 2026

- 152 Maven tests, 20 Chrome shopper scenarios, and 13 Chrome milestone
  scenarios passed. The shopper suite used a controlled chat-provider fixture.
- Chrome verified a named Cart, its shared budget comparison, Ask result
  links, cancellation and confirmation, and a reminder drafted over two turns.
- Live CityU DeepSeek checks returned a calculator card showing HK$30.00
  for 15% of HK$200 and added the exact historical sample milk pack to a
  named test cart. The packaged app used the Windows trust store.
- The file database upgrade and restart preserved the existing cart, retailer
  choice, checked state, and tracked observations.

## Per-rule alert cards and saved switches — 21 November 2026

- 142 Maven tests and 32 Chrome scenarios passed. Chrome checked the reminder
  card's product, interval, and type. Pausing from Current rules persisted
  in the product editor, and resuming returned the rule to On.
- Older reminder and price-rule switches survived file database upgrade and
  restart. Pausing a rule kept its history.

## Alert rule editor and Wishlist actions — 21 November 2026

- 139 Maven tests, 19 Chrome shopper scenarios, and 13 milestone scenarios
  passed. Both Alert tabs render in all three supported languages.
- Tests verified the shared editor, persisted switches, deletion with retained
  history, single-fire and repeat scheduling, exact-trigger history groups,
  and Wishlist action labels. The older-file upgrade preserved price targets
  and history while allowing time-only events without price evidence.

## Shared page styling and product navigation — 21 November 2026

- 132 Maven tests and 19 Chrome shopper scenarios passed. Chrome checked
  card-area navigation, return links to Cart and Wishlist, and the warm
  background and serif headings at four viewport widths. Cart product names
  retained their sans-serif font.

## Cart completion and catalogue colors — 21 November 2026

- 132 Maven tests, 19 Chrome shopper scenarios, 12 milestone scenarios, and
  the Cart restart check passed. Chrome verified the product return link,
  locked completed controls, and reopening imported lists from List actions.
- Discover's browser check verified that category colors survive filtering,
  with neutral category art in high contrast.

## Discover and Cart layout — 20 November 2026

- Maven packaging passed 132 tests with no skips. Chrome passed 19 shopper
  scenarios and 12 milestone scenarios. The file-backed upgrade preserved
  retailer choice, quantity, done state, and tracked prices.
- Chrome checked the desktop sticky budget rail, responsive Cart, keyboard
  quantity edits, done and undo, and the List actions panel at phone width.
  JSON sharing and the offline checklist still passed. The preview captures
  use the checked sample and a saved weekly budget.

## Flat palette, shopping icons, and alert conditions — 20 November 2026

- The UI stage passed 124 Maven tests, 19 Chrome shopper scenarios, 11 Chrome
  milestone scenarios, and a file-backed Cart restart. The chart checks covered
  pointer and keyboard inspection, retailer visibility, and recorded gaps.
- The final Maven build passed 130 tests with no skips. The controlled clock
  covered a combined occurrence waiting for a qualifying live refresh, grouped
  retailer evidence, rollback, and a concurrent refresh. The older H2 schema
  reopened with its price target and event history intact.
- Chrome passed 19 shopper and 12 milestone scenarios. These include the named
  icon controls and touch targets, independent price and combined rule editing,
  personal-data export and reset, and existing independent time reminders. The
  older JAR to current JAR check preserved the Cart and tracked history.

## Mint and coral glass theme — 20 November 2026

- The final build passed 123 Maven tests and 19 Chrome shopper scenarios,
  including mobile chat-history selection and transcript spacing. The mint
  and coral build also passed 11 milestone scenarios and the Cart restart test.

## Market Desk visual refresh — 20 November 2026

- Maven packaging passed 123 tests. Chrome passed 19 shopper and 11
  milestone scenarios. The Discover check verifies the artwork, mobile
  keyboard search, and Cart shortcut. The file-backed Cart restart also passed.

## Market Desk redesign · Retailer history and Alerts — 20 November 2026

- The chart passed 14 focused Maven tests and the Chrome shopper workflow.
  It shows only recorded observations and separates unknown prices and
  tracking periods. The 30-day and retailer filters update the chart and
  table together. Chrome checked English and both Chinese variants.
- The final build passed **123 Maven tests**, **18 Chrome shopper
  scenarios**, and **11 Chrome milestone scenarios**. The fixture covered
  grouped price targets and rearming, existing retailer drop evidence, time
  rule editing, and the combined Alerts screen. Controlled-clock tests check
  one due reminder occurrence and preserved history after dismissal.
- The final build also opened a copy of a file-backed H2 database made by
  the earlier Cart build. It kept Cart quantities and retailer choices, the
  reminder, the old retailer rule and event, and price observations. The
  next accepted live fixture enrolled missing retailers.

## Market Desk redesign · Navigation and Cart — 19 November 2026

- Navigation and Settings passed the focused controller checks and Chrome
  shopper and milestone suites. The suite opens Settings with the keyboard,
  checks the mobile tabs and reduced motion, and saves a budget in Personal data.
- Cart passed 33 focused Maven tests, 17 Chrome shopper scenarios and
  11 Chrome milestone scenarios. The second app imported an editable JSON
  copy. Chrome then downloaded an HTML checklist from a separate fixture app,
  stopped that app, disabled the browser's network and checked another item
  in the file.

## Market Desk redesign · Wishlist — 19 November 2026

- The H2 upgrade and lifecycle suites passed 13 focused tests. The upgrade
  kept old price history and retailer rules and enrolled new retailers from
  live imports. The browser checks passed 18 shopper and 11 milestone
  scenarios, including mobile row actions and batch additions that preserve
  the quantity and retailer for an existing Cart item.

## DeepSeek catalogue tools — 19 November 2026

- Maven packaging passed **114 Java tests**. The provider fixture verified
  natural replies, matched tool call IDs, invalid args and the tool limit.
  Catalogue checks covered ranked results, exact packs, bilingual retailers,
  unknown prices and retrieval times. Chat export includes checked rows.
- Chrome passed **17 shopper scenarios** and **11 milestone scenarios**. The
  Ask workflow checked send and retry, a natural Chinese reply, cheapest-pack
  search, the exact-pack picker, history, and mobile layout.
- Live CityU in Chrome produced a Traditional Chinese introduction, found
  the 236 mL sample milk for HK$7.50 at AEON, and checked the same pack for
  HK$12.00 at `惠康 WELLCOME`. The first live run exposed a bilingual
  retailer lookup failure. The rerun passed after the alias fix.

## Required DeepSeek interpretation — 19 November 2026

- Maven packaging passed **107 Java tests**. The focused checks covered missing
  credentials, unusable provider output, and a retry of the accepted question.
- Chrome passed **16 shopper scenarios** and **11 milestone scenarios**. The
  provider fixture received both an introduction request and a retailer
  follow-up. A simulated outage kept the question for retry, and the product
  comparison page remained available.
- The CityU preview interpreted an introduction request, a milk search, and a
  Wellcome follow-up. Prices came from the historical sample catalogue.

## Ask message delivery and conversational prompts — 18 November 2026

- Maven packaging passed **105 Java tests**. The focused turn checks verified
  that Ask saves the shopper's message before calling the provider and
  requests the reply once, even if two requests arrive together.
- Chrome opened Ask in a fresh session and held the reply request. The sent
  bubble was already visible and present in a separate server read. The user
  could type the next draft while waiting. The checks covered send failure,
  interrupted replies, and retry without a second shopper bubble.
- The CityU preview verified the two-stage flow with “who are u,” a natural
  milk question, and a Wellcome follow-up for the same pack. The preview used
  historical sample prices.

## Ask conversation and layout fixes — 18 November 2026

- Focused Java tests reproduced three failures. An unmatched search cleared
  the selected pack, incomplete provider output cleared pending choices, and
  a stale chat ID dropped the submitted question. All 18 focused tests passed
  after the fixes. Maven packaging passed all **102 Java tests**.
- Chrome passed **15 shopper scenarios** and **11 milestone scenarios**.
  The Ask check held the first send to observe pending feedback, restored
  the draft after a failed send, and retried. It reopened the original chat
  at 375 by 667 px with at least three messages partly or fully in view and
  the composer above mobile navigation.
- The CityU DeepSeek preview answered two separate sample chats in one browser
  session. Reopening the first chat preserved the exact pack for a retailer
  follow-up. The test used the historical sample catalogue.

## J10–J12 combined validation — 18 November 2026

- JDK 21 Maven package passed **101 Java tests** with no failures, errors, or
  skips. Focused checks covered transfer validation, the offline session guard,
  both Chinese variants, retained history, and personal reset. An injected
  foreign-key failure rolled back the reset and kept the session intact.
- Chrome passed **15 shopper scenarios** and **11 milestone scenarios**.
  The shopper suite used the 236 mL milk pack across Ask, sharing, offline
  review, language switching, and history. It checked keyboard operation and
  contrast on desktop and at 375 px, and confirmed a known price stayed on one
  line at that width. The personal-data flow exported each record type, kept
  records on cancellation, and cleared them after confirmation while the
  accepted catalogue remained available.
- The Chrome persistence check upgraded a temporary H2 file from the
  J01–J08 build at `219cc5b`. The catalogue, default list, two checked packs,
  pinned JASONS choice, product-wide tracking, and recorded price survived.
- The browser checks used a controlled price feed and local Ask fallback.
  See the earlier J09 record for the live CityU check. The J10–J12 PlantUML
  sequence passed syntax checking, SVG and PNG rendering, and visual review.

## J10 sharing and J11 offline review — 18 November 2026

The J10 focused Java checks passed. Chrome transferred a versioned file
between two isolated apps, retained a checked item and a pinned retailer,
showed Unknown without local prices, and saved independent edits. Invalid
JSON left the existing lists intact.

The J11 focused Java suite passed 16 checks. Chrome reopened the imported
list with non-local browser requests blocked. It verified the cached estimates
and retrieval time, tested server rejection of edits, refresh, and chat, and
restored editing after exit. All 12 Chrome scenarios passed at this stage.

## J09 chat layout and session history — 17 November 2026

Maven packaging passed all 90 Java tests. The Chrome baseline suite passed
10 scenarios, including the new Ask workflow. The shopper sent a question,
followed up at Wellcome, started a second chat, and reopened the first on
a 375 px viewport. After shortening the mobile chat panel, a Chrome rerun
verified the Send button sits above the bottom navigation.
The browser used the historical sample and disabled the provider key.
Desktop and mobile screenshots are under `target/browser-checks/`.

## J09 conversation check — 17 November 2026

The packaged app was started on loopback with an isolated in-memory catalogue,
the checked historical sample and the CityU model `CS3383/DeepSeek-V4-Flash`.
The live flow asked which product “How much is it?” referred to, found AEON
at HK$7.50 for Kowloon Dairy's 236 mL fresh milk, and answered a follow-up for
the same pack at Wellcome with HK$12.00. Those prices came from the sample.

Java initially rejected the TLS chain. The Windows trusted certificate store
resolved the connection while preserving certificate and hostname verification.
See the [setup guide](../SETUP.md) for the launch options.

## PR #18 review follow-up — 12 November 2026

- Maven package on JDK 21 with the Byte Buddy startup agent passed 88 tests
  with zero failures, errors or skips. Coverage includes next-day summary
  catch-up, enrollment of new LIVE retailers, stop-all and preference persistence.
- Chrome passed 9 baseline scenarios and 11 milestone scenarios.
- The old-to-new H2 upgrade preserved the catalogue and saved list state.
- PlantUML 1.2026.9 regenerated the four views for the revised D01 and reminder flow.

## J05–J08 validation — 6 November 2026

Checked the uncommitted `dev/john` implementation against issue #17 and the
approved D01–D07 rules. The issue remains open; no commit, push or merge is
implied. Environment: JDK 21.0.8, Spring Boot 3.4.5, H2 2.3.232, Node 24.14.0,
Playwright 1.58.2 and the installed Chrome channel.

| Check | Observed result |
|---|---|
| Full Maven package | 86 tests; no failures, errors or skips; runnable JAR built |
| Final full Maven test after UI polish | 86 tests; no failures, errors or skips |
| Baseline Chrome suite | 9 scenarios passed |
| J05–J08 Chrome suite | 11 scenarios passed, including final mobile-price layout check |
| Old-to-new H2 upgrade | Existing catalogue, JASONS selection, quantity, checked state and HK$25.00 line estimate retained |
| UML | Four official PlantUML 1.2026.9 sources checked; SVG/PNG rendered; worker and integrator inspected every view |

Commands from the repository root:

```powershell
$env:JAVA_HOME = 'D:/App/Java/JDK21'
$testAgent = '-DargLine=-javaagent:C:/Users/User/.m2/repository/net/bytebuddy/byte-buddy-agent/1.15.11/byte-buddy-agent-1.15.11.jar'
.\mvnw.cmd $testAgent package
.\mvnw.cmd $testAgent test
$env:BROWSER_CHANNEL = 'chrome'
npm run test:browser
npm run test:milestone
$env:BASELINE_JAR = 'target/upgrade-check/baseline.jar'
npm run test:persistence
.\docs\diagrams\j05-j08\render.ps1 -Java 'D:/App/Java/JDK21/bin/java.exe'
```

The explicit Byte Buddy startup agent avoids a dynamic-attach hang observed
in this environment. It is not an application dependency or runtime setting.
Focused checks also passed: tracking 8, alerts 7, tracking file reopen 1,
budget 13, reminders 8 and reminder file reopen 1. Those are included in the
full suite, not additional tests to add to 86.

The first tracking run exposed an assigned-`@MapsId` wishlist insertion error;
leaving the ID for JPA to derive fixed it. Review regressions cover stale/sample
targets, fresh current targets after history expiry, unchanged-import deduplication,
unknown gaps, snapshot drop evidence, database rollback, snoozed/read summary
exclusion, overnight quiet deferral, concurrent summaries, and restart state.
Pending alert details remain hidden until eligible.

The new browser harness initially needed two fixes: normalize Spring's initial
`;jsessionid` redirect, and use the reminder controls' accessible names. Its
navigation/click promises now fail together so cleanup still runs. The final
run passed. Desktop/mobile screenshots and logs are in ignored
`target/browser-milestone/`; baseline artifacts are in `target/browser-checks/`.
The integrator viewed all four desktop and mobile milestone screenshots.

The upgrade used the old J01–J04 JAR preserved before rebuilding, then the new
J05–J08 JAR against the same isolated temporary H2 file. This was an actual
old-to-new run, not the earlier same-JAR control run. `WRITE_DELAY=0` makes the
harness independent of Windows forced-termination timing.

Limits: all price changes are explicit synthetic/local fixtures. These checks
do not establish current provider prices, every historical database version,
all browser engines, complete accessibility or wider J09–J12 delivery. No normal
user database was used. There is no account, cloud notification, purchase ledger,
promotional-price calculation or price prediction in this milestone.

## Historical records

The table below is the **retired UI-prototype suite**. The J01–J04 implementation
replaces these six-scene checks with real pages, services, and persistence checks.
The 17 October results remain historical evidence, not results for the new code.

| ID | Area | Test | Expected result | Source at prototype stage |
|---|---|---|---|---|
| TC-WEB-001 (superseded) | Web MVC | `GET /` rendered the prototype | `200 OK`, HTML, view `market-desk`, expected page title | Historical `HelloControllerTest.java` |
| [TC-WEB-002](#tc-web-002--retired-prototype-scenes-and-assets) | Web MVC | Root page includes prototype scenes and assets | Sample labels, six scenes, local CSS and deferred JavaScript | [`HelloControllerTest.java` at the prototype commit](https://github.com/LamHeiKit230208993/CS3383-Project/blob/8f94149/src/test/java/com/pricewatch/web/HelloControllerTest.java) |
| [TC-WEB-003](#tc-web-003--retired-search-and-demo-controls) | Web MVC | Root page includes search and demo controls | Visible search label tied to its input, main landmark, navigation, and dialogs | [`HelloControllerTest.java` at the prototype commit](https://github.com/LamHeiKit230208993/CS3383-Project/blob/8f94149/src/test/java/com/pricewatch/web/HelloControllerTest.java) |

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


## TC-WEB-002 — Retired prototype scenes and assets

> Retired case. Use [TC-WEB-001](cases/TC-WEB-001.md) for current page checks.

| Field | Details |
|---|---|
| Test class | `HelloControllerTest` |
| Test method | `rootIncludesPrototypeScenesAndLocalAssets()` |
| Type | Spring MVC slice test |
| Request | `GET /` |

### Purpose and steps

Send `GET /`. Check the sample labels, scene sections, stylesheet reference,
and deferred script reference in the rendered HTML.

### Expected result

- The response includes `UI prototype` and `Sample data`.
- A section exists for each scene: Discover, Compare, List, Saved, Updates,
  and Assistant.
- A stylesheet link points to `/css/market-desk.css`.
- A deferred script points to `/js/market-desk.js`.

### Limits

The test checks asset references, not whether a browser can load or run them.
It does not check scene switching, charts, or visual layout.

Source: [`HelloControllerTest.java` at the prototype commit](https://github.com/LamHeiKit230208993/CS3383-Project/blob/8f94149/src/test/java/com/pricewatch/web/HelloControllerTest.java)


## TC-WEB-003 — Retired search and demo controls

> Retired case. Use [TC-WEB-001](cases/TC-WEB-001.md) for current page checks.

| Field | Details |
|---|---|
| Test class | `HelloControllerTest` |
| Test method | `rootProvidesNamedSearchAndReachableDemoControls()` |
| Type | Spring MVC slice test |
| Request | `GET /` |

### Purpose and steps

Send `GET /`. Check the actual HTML elements for the search label and input,
main landmark, Discover navigation link, and native dialogs.

### Expected result

- A non-empty visible label targets input `product-search`.
- The main landmark has ID `main-content`.
- A navigation element contains a link to `#discover`.
- Native dialogs exist with IDs `settings-dialog` and `quantity-dialog`.

### Limits

This test checks page structure. Browser checks must cover focus, dialog
opening and closing, search results, and quantity changes.

Source: [`HelloControllerTest.java` at the prototype commit](https://github.com/LamHeiKit230208993/CS3383-Project/blob/8f94149/src/test/java/com/pricewatch/web/HelloControllerTest.java)


## Retired Market Desk theme — issue #7

The 17 October 2026 prototype used sample data and temporary browser state. Its
scene navigation, dialogs, history/settings demos, and reset-on-reload behaviour
were replaced by the J01–J04 implementation. This prototype is design evidence,
not working budgets, alerts, tracking, or AI.

The original [theme plan at its prototype commit](https://github.com/LamHeiKit230208993/CS3383-Project/blob/8f94149/docs/Plan/market-desk-theme.md)
remains in Git history. Current screens and limits are recorded in the
[implementation guide](../implementation-baseline-j01-j04.md).
