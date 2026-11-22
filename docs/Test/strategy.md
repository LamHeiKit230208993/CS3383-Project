# Market Desk test strategy

## Scope and release evidence

Cover the local Spring Boot, Thymeleaf, and H2 application from catalogue import
and search through Cart, Wishlist, Alerts, Ask, Settings, and offline HTML.
Use the [case catalog](test-case-catalog.md) for maintained checks and
[history](history.md) for dated results. The [J09–J12 workflows](../Plan/issue-21-workflow-checks.md)
define the shopper tasks and acceptance criteria.

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

## Remaining test plan

| Priority | Work | Evidence and completion criterion |
|---|---|---|
| 1 | Review uncovered branches in the web package, starting with Ask confirmation and catalogue replacement | Add tests for stale or repeated confirmation, failed writes, and replacement without consent where gaps remain. Assert the response and stored state. Rerun JaCoCo and compare covered and total outcomes against the recorded baseline. |
| 2 | Extend condition analysis to selected compound decisions in quantity validation, budget warnings, and Ask authorization of a pending action | Record feasible decision and condition outcomes. Add MC/DC independence pairs for conditions that can vary independently. Document coupled or short-circuited inputs where they affect the analysis. |
| 3 | Measure browser-side code where useful | Run instrumented Chrome flows against Cart, Ask, and the standalone offline checklist. Report JavaScript counters separately and retain offline and keyboard scenarios as acceptance tests. |
| 4 | Make manual usability retests repeatable | Run named Cart and Settings tasks with fresh reviewers at desktop and phone widths. Record the device, task, expectation, observation, revision, and outcome. Add before-and-after evidence when available. |

The baseline identifies gaps for review. Report achieved coverage and any
remaining gaps after the follow-up run.

## Reproduction and sources

Run Maven and the three browser scripts using the [setup guide](../SETUP.md).
For Java coverage, copy the build into an isolated folder and run JaCoCo
0.8.14 with the Maven goals `prepare-agent`, `test`, and `report`, in that
order. If Surefire already has JVM agents configured, preserve JaCoCo's agent
argument in its forked JVM. Open `target/site/jacoco/index.html` and use the XML
report for exact numerators and denominators.

[JaCoCo coverage counters](https://www.jacoco.org/jacoco/trunk/doc/counters.html)
· [NASA tutorial on MC/DC](https://ntrs.nasa.gov/citations/20010057789)
