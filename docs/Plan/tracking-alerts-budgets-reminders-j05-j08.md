# J05–J08: Tracking, alerts, budgets, and reminders

Updated: 29 October 2026.

Status: The team agreed to group J05–J08 in one milestone issue (issue #17).
The D01–D07 decisions below were agreed on 31 October 2026 and recorded before
dependent implementation. Implementation progress is tracked in that issue.

Read the [project scope](../project-scope.md), [job plan](features.md), and
[implemented J01–J04 contract](../implementation-baseline-j01-j04.md) first.
The project scope controls feature decisions. This file controls the next
milestone's delivery plan and agreed implementation rules.

## Goal and boundaries

**Save a product → explicitly track it → retain observations after valid updates
→ manage in-app alerts → review a planned budget → act on a replenishment reminder.**

Use the merged PR #15 baseline: one local Spring Boot app, Thymeleaf forms, and
an H2 file database. Preserve search, retailer choice, exact decimal amounts,
unknown-price handling, import rollback, and saved-list behaviour.

- Saving a wishlist entry, tracking, adding to My list, and recording a purchase
  are distinct actions. A reminder does not prove household use or a purchase.
- Retain history only for explicitly tracked exact products and retailers.
  Keep retrieval time separate from a supplied source date. The current feed's
  source date is unknown; a later retrieval alone does not prove a new price.
- Deliver alerts, summaries, and reminders inside the running app. Background
  device/cloud notifications, accounts, and cloud storage remain outside this batch.
- J07 adds planned budgets and category totals to existing list calculations.
  It does not add purchase accounting or claim actual spending/savings.
- J09 chat, J10 transfer, J11 offline reopening, and full J12 completion remain
  later work. History inspection needed to verify J05 belongs here; full history
  charts remain later work subject to R10's data condition.
- R22 change thresholds remain conditional on an agreed comparison price.
  Other conditional rows, including R24, R29, and R35, are not approved by batching.

## Workstreams and dependency order

| Workstream | Deliverable | Depends on | Required checks |
|---|---|---|---|
| J05 — wishlist and tracking | Separate saved and tracked state; retained selected-product observations; source/date details | J01–J04; tracking decisions below | Restart preserves choices/history; saving alone does not track; stopping tracking stops new collection; failed imports add no observations |
| J06 — price alerts | Target/drop rules, repeat prevention, quiet hours, snooze, morning summary | J05 observations and agreed alert/time rules | A target uses a known current price; a drop uses a stated earlier observation for the same product/retailer; repeated updates do not duplicate events |
| J07 — planned budgets | Weekly/monthly budget settings, list estimate warning, category totals | J04 totals and agreed budget rules | Quantities and retailer choices affect estimates; unknown prices mark totals incomplete; category amounts reconcile with the known subtotal |
| J08 — replenishment reminders | User-set interval/use input; due state; add-to-list, clear, and snooze | J04; J06 notification behaviour and agreed reminder rules | Product/quantity survive actions and restart; clear/snooze set the agreed next due time; unavailable prices remain unknown |

J05 and J07 can proceed in parallel once their own decisions are resolved.
J06 follows J05; J08 follows the shared notification behaviour. UI sketches and
test-case design can proceed while independent storage work runs.

Use one milestone issue with checklists or linked sub-issues. Separate PRs for
the workstreams are recommended. Assign one owner for each workstream and one
integrator before implementation. Agree ownership of shared import, list,
navigation, and notification code before parallel edits.

## Shared records and operations

These are logical contracts to settle before connected implementation. They are
not existing Java classes, database migrations, method signatures, or web routes.

| Record | Required meaning |
|---|---|
| Wishlist entry | Exact product reference and saved state, independent of tracking |
| Tracking choice | Exact product, retailer selection, enabled state, and tracking period |
| Price observation | Exact product/retailer, decimal price or explicit unknown state, retrieval/source provenance, and identity used for duplicate prevention |
| Alert rule and event | Rule type/settings; referenced observations; eligibility and visible-event state; persisted repeat/snooze information |
| Budget settings/result | Weekly/monthly choice, planned limit, known estimate, missing-price count, category breakdown, and period meaning |
| Reminder | Product, quantity, user-supplied interval/use input, due state, next due time, and clear/snooze state |

Agree inputs and result/error states for save/unsave, enable/stop tracking,
record observations, evaluate alerts, change budget, and create/clear/snooze a
reminder. Reuse J04's add-to-list validation for reminder actions, including its
quantity limit and conflict when an existing item has a different retailer.

Current catalogue replacement deletes retailer listings and offers. History must
survive replacement through separate retained records; do not attach its lifetime
to those deleted listing rows. Agree the transaction boundary for catalogue,
observations, and alert events. A failed import must leave accepted data and new
history/events unchanged. A visible import success notice must follow a successful
commit. If later notification processing fails after that commit, retain the
accepted import and report/retry the notification failure under the agreed policy;
do not claim that an already committed import was rolled back. The transaction
and retry design still needs agreement before implementation.

Use one notification policy for alerts, summaries, and reminders. Define time-zone
and clock handling once, with controllable time in tests. Store due/event state
so restart does not reset snooze or create duplicate notifications. A closed app
must not be reported as having collected data or delivered a notification.

## Decisions before dependent work

All rows below were **decided on 31 October 2026**, recorded before dependent
implementation. The chosen rule and examples are binding for the J05–J08 code,
tests, and UML. Do not introduce separate defaults.

| ID | Decision | Chosen rule (31 Oct 2026) |
|---|---|---|
| D01 | Which retailer(s) are tracked? Seeded from catalogue? | Enabling tracking tracks **all retailers currently listed** for that exact accepted product. The seed is taken from the accepted catalogue at enable time: one tracking row per (product, retailer). Later catalogue additions for the same product are not auto-tracked until tracking is re-enabled. Example: enabling tracking on `P000000124` (JASONS, WELLCOME, AEON, …) stores one enabled choice per retailer then listed. |
| D02 | Retention, stop/re-enable, delete history, missing products | History is kept until tracking is turned off; stopping **keeps** existing observations but **stops** new collection. Re-enabling starts a **new tracking period** (new `periodId`); observations carry their period. Deleting history is not offered in this batch. A product that leaves the catalogue keeps its history and stops receiving new observations (no new listing to observe). Example: track → import A → stop → import B (not observed) → re-enable (period 2) → import C (observed in period 2). |
| D03 | Observation identity, duplicates, unknown prices, unknown dates | Observation identity = (product, retailer, import checksum, tracking period). A repeated identical import (same checksum) adds no new observation. An unknown price is stored as an explicit unknown observation (`price = null`, `known = false`) **for provenance only**: it is excluded from target/drop comparisons, trend/gap hints, repeat-reset, and budget arithmetic, and is never stored as `0` or interpolated. Source date stays unknown (the feed supplies none); retrieval time is recorded separately. Example: two identical sample imports produce no second observation; a null price is recorded but cannot fire a drop. |
| D04 | Target boundary, earlier comparison, first-check alert, repeat reset | Target fires when the current **known** price is **at or below** (≤) the target amount. A price-drop alert compares the new observation against the **latest prior known** observation for the **same product/retailer**; an unknown value can be the current state (no drop) but can never be the earlier comparison. If the target is already met when the rule is created, it alerts on the next successful evaluation. Repeat reset happens only when a new distinct **known** observation arrives for that pair. No amount/percentage thresholds (R22) in this batch. Example: target 10.00, price 9.95 → fires; price 9.95 then 9.95 (same import) → no second event. |
| D05 | Time zone, quiet hours, snooze, summary, missed runs, scheduling | Clock = injected `Clock` bean, zone `Asia/Hong_Kong`. Quiet hours **22:00–07:00 HKT**: alerts are suppressed (not delivered, not marked visible) during that window. Snooze = **24 hours** from the action time. Morning summary appears in-app on the **first page visit at or after 07:00** that day and covers audible alerts for the day. A period while the app was closed is shown as "while the app was closed", never as delivered. Evaluation runs on page load/refresh (no background scheduler); a closed app collects and delivers nothing. Example: alert generated at 23:10 is held and shown at/after 07:00 with the summary; snoozing at 09:00 hides it until 09:00 the next day. |
| D06 | Budget estimate vs weekly/monthly budget; reset, warning, category level, missing prices | The budget applies to the **whole My list estimate** (J04 lowest-price logic). Weekly vs monthly is a **label** plus the reset boundary for "period-to-date" display; the estimate itself is the current list. Warning when the **known subtotal exceeds** the planned limit. Category breakdown uses **category2**, with unmatched items in **"Other"**; category amounts sum to the known subtotal. Unknown prices keep the estimate **"incomplete"** (existing `unknownItemCount`) and the warning is shown only on the known part. Estimates are never labelled actual spending. Example: limit 100, known subtotal 112 → warning; one unpriced item → "incomplete", known categories still reconcile. |
| D07 | Reminder interval/use input, initial due, clear vs snooze, next date, add-to-list effect | A reminder stores a **user-set interval in days** (whole number ≥ 1). Initial due = creation time + interval. **Clear** deletes the reminder. **Snooze** moves the due time to now + 24h. Adding to the list (via `ShoppingService.add`, reusing its 1–999 and retailer-conflict rules) clears the reminder and sets the next due = now + interval. Product and quantity are preserved across actions and restart. Example: interval 7 days, created day 0 → due day 7; snoozed on day 7 → due day 8; added to list on day 8 → cleared, next due day 15. |

The [feasibility overview](../feasibility-review-simple.md#5-open-decisions)
retains wider-project questions; this milestone's answers are recorded above.

## Acceptance and issue closure

Keep source fixtures unchanged. Use explicit constructed observations and a
controlled clock for date/threshold tests; synthetic history is test data.

1. Save a product, explicitly enable tracking, and accept two distinguishable
   observations. Verify exact product/retailer identity, retained history, dates,
   and original source offers. Restart and read the same state.
2. Exercise repeat and failed imports, stopped/re-enabled tracking, unknown
   prices, and removed products under the agreed D01–D03 rules.
3. Trigger a target event from one known observation and a drop event from a valid
   pair. Verify no unsupported drop with insufficient history, no duplicate event,
   quiet hours, snooze, summary, and restart/missed-run rules.
4. Configure a budget and review category totals. Change quantity or retailer and
   refresh a price. Verify the known subtotal and warning; unavailable amounts
   keep the estimate incomplete. Checked items retain J04's total behaviour.
5. Make a reminder due. Add the chosen quantity to My list, then test clear and
   snooze independently. Verify due dates, item conflicts, and persisted state.
6. Run the combined flow: a tracked price update creates the expected alert,
   updates the list estimate/budget result, and a reminder adds the intended item.
   Test each feature separately too; a reminder need not enable price tracking.
7. Preserve native form, keyboard, mobile, and planned-feature behaviour. Add
   focused tests and case documents with each PR; run the full suite and saved
   browser checks against the integrated batch before issue closure.

Record results in the [test catalogue](../Test/test-case-catalog.md), with source
links, commands, dates, and limitations. Update implementation documentation and
UML when the new behaviour exists. The accepted diagrams in
`docs/diagrams/baseline/` remain the implemented J01–J04 view until then.

Close the shared issue when J05–J08 meet the agreed checks, their PRs are merged,
and the integrated evidence is recorded. A planned screen or this document alone
does not complete a job.

## Implementation notes and known risks (31 October 2026)

These were accepted for this milestone and recorded here rather than fixed:

- **Import transaction boundary.** Observations and alert evaluation run in
  `PostImportProcessor` after `CatalogReplacementService.replace` has committed.
  A failure there does not roll back the accepted import, matching this plan's
  rule (lines 78-83). Trade-off: if post-import work throws, the import is kept
  but observations/alerts may be missing until the next import. A retry/backoff
  policy is future work.
- **No trend prediction.** J05-J08 keep dated observations and show gaps only.
  R10 charts and R35 buy/wait advice remain out of scope; no interpolation is
  performed (see D03). Unknown prices never affect target/drop, repeat-reset, or
  budget arithmetic.
- **Scheduling.** No background scheduler. Reminders and held alerts are
  evaluated on page load/refresh against the injected Clock, so a closed app
  collects and delivers nothing (D05/D07).
- **UML renderer.** Exports use the local VS Code PlantUML 1.2024.3 JAR via
  `-o <dir> -t<format>` (the long `--output-dir --format` form produced no files
  locally). The baseline 1.2026.9 exports under `docs/diagrams/baseline/` are
  unchanged; the J05-J08 exports live in `docs/diagrams/j05-j08/`.
- **Browser checks.** Playwright headless-shell was not downloaded on this
  machine; the run used the installed Chrome channel (set `BROWSER_CHANNEL=chrome`).
  CI should run `npx playwright install chromium` or set the channel explicitly.
