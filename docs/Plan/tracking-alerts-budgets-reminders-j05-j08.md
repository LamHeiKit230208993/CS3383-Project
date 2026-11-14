# J05–J08: Tracking, alerts, budgets, and reminders

Updated: 12 November 2026.

Status: The team grouped J05–J08 in one milestone issue (#17). Decisions D01–D07
are agreed (see below). The implementation is in progress; this document remains
the authority for the milestone's delivery plan and agreed implementation rules.

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

All rows below are **decided** (decision date: 12 November 2026). Each rule states
its examples and the acceptance checks it affects. Agents must follow these
defaults; do not choose separate ones.

| ID | Decided rule | Examples | Acceptance checks |
|---|---|---|---|
| D01 | Track **all** priced retailer listings of a tracked product. Enabling tracking is an explicit per-product action; it is **not** seeded automatically from the accepted catalogue. | Tracking "milk" records observations for JASONS/AEON/WELLCOME priced listings only; a listing without a price is skipped. | 1, 2 |
| D02 | History is kept in separate tables that survive catalogue replacement. Untracking stops new collection but keeps existing history until the user deletes it. Inactive/missing products keep their history; new observations resume only when a priced listing returns. Deleting a wishlist (saved) entry does **not** delete tracking or history. | Replace the catalogue → old observations remain readable. Remove a saved product → tracking state and history stay. | 1, 2 |
| D03 | Store **one observation per (product, retailer) per accepted import** with a known price; unknown prices add no row. A unique key `(product, retailer, checksum)` means an **unchanged repeated file does not duplicate** rows. A drop alert compares against the **most recent distinct prior price**. | Two changed imports → two rows per pair. Re-importing the identical file → no new row. Identical prices across imports do not trigger a drop. | 2, 3 |
| D04 | A TARGET fires when the current price is **≤** target. It is checked immediately on rule creation and after each valid import. Repeat reset: one event per newly observed observation (tracked by `lastFiredObservationId`); the same observation never fires twice. A DROP needs an earlier distinct observation and fires when the latest price is strictly lower. | Target 10 fires at 10.00 and 9.50, not at 10.50. A drop needs a real earlier higher price. | 3 |
| D05 | One injectable `Asia/Hong_Kong` `Clock`. Morning summary at **08:00**. Quiet hours **22:00–07:00**: events are still stored, but banners are suppressed and released after 07:00. Alert-event snooze **8 hours**. No background scheduler; evaluation happens on request or after an import. A closed-app period is never reported as a delivered notification. | Create a rule at 23:00 → event stored, hidden until 07:00. Reopening after a missed 08:00 shows the summary, not a claimed 08:00 delivery. | 3 |
| D06 | Single budget setting (WEEKLY **or** MONTHLY) with a positive limit. Compare the current My-list **known subtotal** to the limit; warn when the known subtotal exceeds it. Category totals use `category2` → fallback `category1` → "Uncategorised", and reconcile with the known subtotal. Unknown prices keep the estimate incomplete. There is **no rollover**: this is a live snapshot of a planned list, not dated spending. | AEON milk 7.50 vs explicit JASONS 12.50: the explicit choice keeps 12.50. Missing selected price → incomplete. | 4, 6 |
| D07 | Reminder interval (days) and configurable snooze (days). Initial next due = creation time + interval. **Clear** deactivates the reminder. **Snooze** adds the snooze interval to the next due date. Adding to My list does **not** change reminder state. Due state is computed from the `Clock`. | Interval 10, snooze 2: due at +10 days; snooze moves it to +12. Adding to the list leaves the due date unchanged. | 5, 6 |

The [feasibility overview](../feasibility-review-simple.md#5-open-decisions)
retains wider-project questions. This milestone's answers are recorded here and
linked from that overview. Scope changes must also update the scope and job plan.

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
