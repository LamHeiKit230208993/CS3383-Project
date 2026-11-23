# Market Desk test strategy

## Scope and release evidence

Cover the local Spring Boot, Thymeleaf, and H2 application from catalogue import
and search through Cart, Wishlist, Alerts, Ask, Settings, and offline HTML.
Use the [case catalog](test-case-catalog.md) for maintained checks and
[history](history.md) for dated results. The [J09–J12 workflows](../Plan/issue-21-workflow-checks.md)
define the shopper tasks and acceptance criteria.

Use the [system design guide](../guide/system-guide.html) from Eric's
`eric/final-docs` revision `80966cd` to map components to integration tests.
The approved localhost-only setup governs the request guard and the browser
test environment. See [Setup](../SETUP.md) for the active configuration.

## Integration strategy and rationale

Use incremental integration, primarily bottom-up. Test price parsing, exact
arithmetic, budget rules, and tool argument validation first. Run the real
services with isolated H2 and exercise the web boundary with MockMvc.
Finish with the packaged app, a real Chrome session, and controlled feed and
chat responses. This order makes a bad calculation or invalid write easier
to locate before it appears as a browser failure.

Derive system and manual acceptance tests top-down from shopper goals.
Follow an exact pack from discovery to comparison and Cart, then through
transfer or offline use. Repeat the important steps on desktop and mobile
with keyboard input, the supported languages, and high contrast. Use a
limited live CityU check to confirm provider compatibility. Run repeatable
regressions with a deterministic local provider fixture.

## Test case design

| Technique | Inputs or events | Expected result |
|---|---|---|
| Equivalence partitioning | Valid versioned Cart JSON, malformed JSON, and a Cart with an unavailable retailer price | Valid data creates a separate copy. Malformed data preserves saved lists. An unavailable price displays Unknown. |
| Boundary values | Cart quantities 0, 1, 999, and 1000. Budget estimate equal to the limit. | Quantities 1 through 999 are accepted. The exact budget limit is within budget when all prices are known. |
| Decision tables | Budget set or absent, prices known or incomplete, and known subtotal above or at the limit | Check the applicable budget state for each combination. Incomplete estimates retain their warning. |
| State transitions | Ask pending, provider outage, retry, confirmation or cancellation | Retry keeps the question and applies a saved action once. Cancellation leaves the Cart or rules unchanged. |
| Failure injection | Database write failure during catalogue replacement | Roll back the imported catalogue and preserve the accepted metadata and Cart. |

Maintain both expected and observed results. Record the requirement or case
ID, revision, setup, input, steps, outcome, and evidence location. Repeat the
relevant check after a fix, then run the applicable regression suites.

## Coverage decisions

The coverage target is 100% of measured Java source lines and branch
outcomes. Run the focused JUnit cases and Spring Boot integration tests in
the same Maven suite. Keep all production Java classes in scope. Report the
actual counters and identify source paths that cannot occur through current
application inputs.

- Line coverage reports executed source lines. JaCoCo branch coverage counts
  bytecode branch outcomes, including those from conditional and switch
  statements. Read the source when assessing a particular compound decision.
- Decision coverage checks both outcomes of a Boolean expression. Condition
  coverage checks true and false outcomes of its individual conditions.
  Condition/decision coverage (C/DC) requires both.
- Modified condition/decision coverage (MC/DC) also checks that each individual
  condition can independently change the decision outcome. Record independence
  pairs from a truth table and account for short-circuit evaluation.
- The instrumented Maven run measured the original Java suite only. Browser
  execution, JavaScript, Thymeleaf markup, and condition independence require
  their own evidence. Use the [budget example](cases/TC-BUDGET-001.md#targeted-decision-coverage)
  as the format for manual condition analysis.

## Coverage follow up

| Priority | Work | Verified result |
|---|---|---|
| 1 | Review uncovered branches in Ask confirmation and catalogue replacement | Added cases for stale previews, invalid and repeated confirmation, a failed confirmation write, missing consent, and a failed refresh. Checked the HTTP result and saved state with H2 where the case used real persistence. [Ask](cases/TC-CHAT-002.md) · [Catalogue](cases/TC-CATALOG-001.md) |
| 2 | Check selected compound decisions | Maintained tests document independence pairs for quantity bounds, budget progress and warning guards, and Ask authorization of an action. The case files identify inputs that Java or Thymeleaf short-circuit. [Cart](cases/TC-LIST-001.md) · [Budget](cases/TC-BUDGET-001.md) · [Ask](cases/TC-CHAT-002.md) |
| 3 | Measure browser-side execution | Chrome recorded V8 source ranges for Cart, Ask, and the offline inline script. Two Node unit tests check the range calculation. The run also caught a mobile history layout regression, and the updated browser case checks the price and source cells separately. [Offline Cart](cases/TC-OFFLINE-001.md) · [History](cases/TC-HISTORY-001.md) |
| 4 | Make usability retests repeatable | An agent completed and documented named Cart and Settings tasks at desktop and phone widths. The case records exact viewports, observed friction, and the scroll-to-expose retest. [Walkthrough](cases/TC-USABILITY-001.md) |

The [dated results](history.md) compare the same production Java source before
and after the follow-up. Continue reviewing remaining uncovered branches when
they affect shopper behaviour. The compound-decision analysis covers the
listed guards. An independent human walkthrough on physical devices remains
available for a later usability round.

## Reproduction and sources

Run Maven and the three browser scripts using the [setup guide](../SETUP.md).
For Java coverage, copy the build into an isolated folder and run JaCoCo
0.8.14 from that copy. If Surefire has other JVM agents configured, preserve
JaCoCo's argument in the forked JVM.

```powershell
.\mvnw.cmd org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.14:report
```

Open `target/site/jacoco/index.html` and read the XML for exact counts. To
measure JavaScript as a separate metric, set `BROWSER_JS_COVERAGE=1` before
`npm run test:browser`. The report combines V8 ranges from the exercised
pages and shows the executed share of each script's UTF-16 source units.
The offline measurement covers its inline script. Chromium may lose earlier
coverage on a renderer swap. A passing run requires all three target scripts.
Check the range calculation with `node --test src/test/browser/javascript-coverage.test.cjs`.

[JaCoCo coverage counters](https://www.jacoco.org/jacoco/trunk/doc/counters.html)
· [NASA tutorial on MC/DC](https://ntrs.nasa.gov/citations/20010057789)
· [Playwright coverage API](https://playwright.dev/docs/api/class-coverage)
