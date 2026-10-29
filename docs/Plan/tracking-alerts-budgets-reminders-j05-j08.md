# J05–J08: Tracking, alerts, budgets, and reminders

Updated: 29 October 2026.

Status: The team agreed to group J05–J08 in one milestone issue. Implementation
has not started in this baseline. This plan does not claim that an issue has
already been created. Add its number here when it exists.

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

All rows below are **open**. Record the chosen rule, examples, decision date,
and affected acceptance checks here before implementing the dependent behaviour.
Agents must not choose separate defaults. Unaffected work can continue.

| ID | Decision needed | Blocks |
|---|---|---|
| D01 | Which retailer(s) are tracked? Is enabling tracking seeded from the accepted catalogue? | J05 tracking contract |
| D02 | Retention, stopping/re-enabling tracking, deleting history, and handling missing/inactive products | J05 lifecycle |
| D03 | Observation identity and duplicate rule; unchanged/repeated files, unknown prices, and unknown source dates | J05 recording; J06 comparisons |
| D04 | Target boundary; earlier comparison observation; first-check alert; repeat reset; any conditional amount/percentage thresholds | J06 evaluation |
| D05 | Time zone, quiet-hour boundaries, snooze duration, summary content/time, missed-run/restart behaviour, and refresh/check scheduling | J06 delivery; shared J08 timing |
| D06 | How one current list estimate relates to weekly/monthly budgets; reset/rollover, warning boundary, category level/fallback, and missing-price warnings | J07 budget rules |
| D07 | Reminder interval/use input, initial due time, clear versus snooze, next-date calculation, and whether adding to the list changes reminder state | J08 lifecycle |

The [feasibility overview](../feasibility-review-simple.md#5-open-decisions)
retains wider-project questions. Agree this milestone's answers here and link
them from that overview. Scope changes must also update the scope and job plan.

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
