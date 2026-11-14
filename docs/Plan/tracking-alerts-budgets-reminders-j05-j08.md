# J05–J08: Tracking, alerts, budgets, and reminders

Updated: 6 November 2026.

Status: [Issue #17](https://github.com/LamHeiKit230208993/CS3383-Project/issues/17)
groups J05–J08 in one milestone. The user approved the D01–D07 rules on
6 November 2026. The local implementation passed its integrated checks on that
date: 86 Java tests, 20 browser scenarios and the old-to-new H2 upgrade check.
See [test history](../Test/history.md). Commit/PR review and merge remain;
the issue is still open. The closure checks below remain applicable.

Read the [project scope](../project-scope.md), [job plan](features.md), and
[implemented J01–J04 contract](../implementation-baseline-j01-j04.md) first.
The project scope controls feature decisions. This file controls this
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
- Reuse J04's effective-price calculation: an explicit retailer choice uses only
  that retailer's current price; Automatic lowest uses a current lowest known
  listing. If the selected retailer's price is missing, retain the choice and
  report an unknown amount and incomplete total. Budget calculations must not
  silently switch the retailer.
- J09 chat, J10 transfer, J11 offline reopening, and full J12 completion remain
  later work. History inspection needed to verify J05 belongs here; full history
  charts remain later work subject to R10's data condition.
- Listing R10, R22, or R35 in the issue does not approve all related features.
  Full history charts remain later work under R10's dated-record condition.
  Amount/percentage change thresholds under R22 require an agreed comparison
  rule in D04 before implementation. Buy/wait advice under R35 is not included
  automatically and retains R16's evidence limits. Other conditional rows,
  including R24 and R29, are not approved by batching.

## Workstreams and dependency order

| Workstream | Deliverable | Depends on | Required checks |
|---|---|---|---|
| J05 — wishlist and tracking | Separate saved and tracked state; retained selected-product observations; source/date details | J01–J04; tracking decisions below | Restart preserves choices/history; saving alone does not track; stopping tracking stops new collection; failed imports add no observations |
| J06 — price alerts | Target/drop rules, repeat prevention, quiet hours, snooze, morning summary | J05 observations and agreed alert/time rules | A target uses a known current price; a drop uses a stated earlier observation for the same product/retailer; repeated updates do not duplicate events |
| J07 — planned budgets | Weekly/monthly budget settings, list estimate warning, category totals | J04 totals and agreed budget rules | Quantities and retailer choices affect estimates; unknown prices mark totals incomplete; category amounts reconcile with the known subtotal |
| J08 — replenishment reminders | User-set interval/use input; due state; add-to-list, clear, and snooze | J04; J06 notification behaviour and agreed reminder rules | Product/quantity survive actions and restart; clear/snooze set the agreed next due time; unavailable prices remain unknown |

J05 and J07 are independent workstreams under the agreed rules below.
J06 follows J05; J08 follows the shared notification behaviour. UI sketches and
test-case design can proceed while independent storage work runs.

Use one milestone issue with checklists or linked sub-issues. Separate PRs for
the workstreams are recommended. Assign one owner for each workstream and one
integrator before implementation. Agree ownership of shared import, list,
navigation, and notification code before parallel edits.

## Shared records and operations

These are logical contracts. Actual classes, operations and web routes are
documented in the [implementation guide](../implementation-j05-j08.md).

| Record | Required meaning |
|---|---|
| Wishlist entry | Exact product reference and saved state, independent of tracking |
| Tracking choice | Exact product, retailer selection, enabled state, and tracking period |
| Price observation | Exact product/retailer, decimal price or explicit unknown state, retrieval/source provenance, and identity used for duplicate prevention |
| Alert rule and event | Rule type/settings; referenced observations; eligibility and visible-event state; persisted repeat/snooze information |
| Budget settings/result | Weekly/monthly choice, planned limit, known estimate, missing-price count, category breakdown, and period meaning |
| Reminder | Product, quantity, user-supplied interval/use input, due state, next due time, and clear/snooze state |

Keep inputs and result/error states consistent for save/unsave, enable/stop tracking,
record observations, evaluate alerts, change budget, and create/clear/snooze a
reminder. Reuse J04's add-to-list validation for reminder actions, including its
quantity limit and conflict when an existing item has a different retailer.

Current catalogue replacement deletes retailer listings and offers. History must
survive replacement through separate retained records; do not attach its lifetime
to those deleted listing rows. Catalogue replacement, observations and price-rule
events share one transaction. A failed import must leave accepted data and new
history/events unchanged. A visible import success notice must follow a successful
commit. If later notification processing fails after that commit, retain the
accepted import and report/retry the notification failure under the agreed policy;
do not claim that an already committed import was rolled back. Summary checks
use a separate transaction and can retry on a later active-app check.

Use one notification policy for alerts, summaries, and reminders. Define time-zone
and clock handling once, with controllable time in tests. Store due/event state
so restart does not reset snooze or create duplicate notifications. A closed app
must not be reported as having collected data or delivered a notification.

## Decisions before dependent work

D01–D07 were agreed with the user on 6 November 2026. D01 was revised on
12 November 2026 after PR #18 review. These are the rules
for this implementation; later changes need corresponding examples and tests.

| ID | Agreed rule | Example / required check |
|---|---|---|
| D01 | Track one retailer or opt into all retailers for an exact product. Product-wide tracking enrolls current listings and new retailers on accepted LIVE imports. Each retailer keeps its own observations and rules. Wishlist and shopping-list choices are independent. | Tracking JASONS alone does not react to AEON. With all retailers enabled, AEON joins when first listed. Stop all ends collection and future enrollment while keeping retained history. |
| D02 | Retain observations for 90 days. Untracking stops collection but retains history. Re-enabling starts a new period; old-period prices cannot trigger a drop. Provide explicit history deletion. Keep minimal last-value state for deduplication after observation expiry. | Restart preserves history; re-enable does not compare against the previous period. Expiry does not make an unchanged re-import a new price observation. |
| D03 | Record only changed prices after successful LIVE imports. Skip consecutive unchanged prices; record transitions to unknown as gaps. Keep retrieval time/provenance, never invent a source date. SAMPLE imports neither seed live history nor generate live price alerts. | Duplicate imports add nothing; invalid/database-failed imports roll back observations/events; no comparisons across unknown gaps. |
| D04 | Target means known price <= target. First known qualifying price can trigger once. Rearm after price rises above target. A drop needs two adjacent known observations in the same tracking period with a lower current price. No configurable amount/percentage thresholds or buy/wait advice. | Repeated qualifying prices do not repeat target events. Unknown prices cannot trigger drops or cross a gap. |
| D05 | Use Asia/Hong_Kong. Defaults: quiet 22:00–08:00, summary 08:00, event/reminder snooze 24 hours. Price refresh remains manual. Active-app checks run every minute and on relevant pages. Summary catches up once on an eligible active-app check, labelled with actual generation time; it does not claim delivery while closed. Persist snooze/repeat/summary state. | Clock-controlled quiet-hour boundaries, overnight windows, snooze and restart; pending events become visible after quiet hours, without duplicate delivery. |
| D06 | Compare current My list estimate with a selected WEEKLY or MONTHLY planning limit. No purchase accumulator or rollover. Warn when known subtotal > limit; when incomplete and not already above limit, overall budget result is unknown. Use English category level2 or Uncategorised. | JASONS choice remains JASONS; category known amounts sum to known subtotal; checked items stay included. |
| D07 | User chooses interval in days and quantity. Initial due = creation + interval. Clear means handled and next due = action time + interval; snooze moves current due by24h without changing interval. Add-to-list alone does not clear, track, or record purchase. | Preserve quantity/product; existing retailer conflicts use ShoppingService.add errors; clear/snooze/restart obey due dates. |

Decisions D04 and D05 use persisted in-app events. Catalogue replacement,
observation recording, and price-rule evaluation must commit together. Display
and due-event processing happen only while the app runs. A later display/check
failure must not claim to undo a committed catalogue. A sample catalogue may
still be used for labelled screen demonstrations and isolated constructed tests.

The [feasibility overview](../feasibility-review-simple.md#5-open-decisions)
retains wider-project questions. Keep this milestone's agreed answers here and link
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
   With the historical sample, selecting JASONS for milk uses HK$12.50 per pack
   even though AEON lists HK$7.50. If JASONS becomes unpriced, retain JASONS and
   show an incomplete estimate. Switching explicitly to Automatic lowest may
   then use AEON. These are regression examples, not current shopping prices.
5. Make a reminder due. Add the chosen quantity to My list, then test clear and
   snooze independently. Verify due dates, item conflicts, and persisted state.
6. Run the combined flow: a tracked price update creates the expected alert,
   updates the list estimate/budget result, and a reminder adds the intended item.
   Test each feature separately too; a reminder need not enable price tracking.
7. Preserve native form, keyboard, mobile, and planned-feature behaviour. Add
   focused tests and case documents with each PR; run the full suite and saved
   browser checks against the integrated batch before issue closure.

Maintain coverage and case links in the [test catalogue](../Test/test-case-catalog.md)
and detailed procedures in `docs/Test/cases/`. Record commands, dates, outcomes,
and limitations in [test history](../Test/history.md). Update implementation documentation and
UML when the new behaviour exists. The accepted diagrams in
`docs/diagrams/baseline/` remain the historical J01–J04 view. The
[current diagram index](../diagrams/README.md) links the J05–J08 extension and
states its review status.

Close the shared issue when J05–J08 meet the agreed checks, their PRs are merged,
and the integrated evidence is recorded. A planned screen or this document alone
does not complete a job.
