# Alerts and daily price refresh

[Issue #27](https://github.com/LamHeiKit230208993/CS3383-Project/issues/27)
unifies price rules and time reminders for the exact pack. Use the
[setup walkthrough](SETUP.md#try-wishlist-alerts-and-budget) to try the app.

## Rules and evidence

Open **Set rules** from Wishlist or Alerts. A price rule needs a saved pack.
A calendar-only rule can stay active when its product leaves Wishlist.
Price rules support a listed price at or below a target, a drop of at least
an HKD amount, or a drop of at least a percentage. New price rules default to
Daily and stay enabled until switched off. A One time rule completes after
its first event. A combined rule waits once its calendar condition is due
and fires when a subsequent live refresh meets its price condition.

A drop compares the current and previous accepted live price for the same
retailer and pack. For example, HK$12.00 to HK$9.00 meets either HK$3.00 or
25%. Missing prices and a first live listing cannot show a drop.
Percentage comparisons require a previous price above zero.
Sample and Demo records do not provide live evidence.
One event groups the qualifying retailers and records the two listed prices
used in each drop comparison. A repeat check on the same accepted observation
does not create a second event.

Existing product-target and retailer-specific rules retain their own cadence,
repeat prevention, and evidence. Alerts history can outlive its rule. The
app shows events in Hong Kong time. Previous morning summaries remain in
history and personal-data export. The scheduler no longer creates summaries
or applies quiet hours.

## Daily import

On startup and the first normal page visit on a new Hong Kong date, the app
checks whether a daily live refresh is due. A background task uses the same
validated import path as the manual **Refresh live prices** button. An idle
app checks when used again. A successful manual refresh satisfies that day's
check, and overlapping automatic and manual requests share the same download.
The daily attempt state survives restart.

The app keeps a Sample or Demo catalogue until a manual live refresh succeeds.
A failed download or invalid response preserves the accepted catalogue and
creates no price-rule event. Manual retry remains available. Replacing the
catalogue, recording live observations, and saving related price-rule events
share one transaction. The `daily_refresh_state` row stores the Hong Kong
date and the attempt outcome. The retrieval time records when the app got the feed.
The source price date is unknown when the provider omits it.

## Upgrading saved reminders

The upgrade reads `replenishment_reminders` and `reminder_occurrences` and
transfers rules to `watch_rules` and history to `product_target_events`.
Source IDs let a repeated upgrade recognize migrated records. The old tables
remain on disk after their rows are transferred. The workspace database check
found zero rows in both tables. Use a separate populated fixture for upgrade validation.

Migrated time rules retain their interval, next due time, switch state, and
snooze. Their occurrences retain timestamps and dismissal state, including
history whose rule was deleted. A migrated Cart action keeps the original
quantity and pinned retailer. If the retailer has no known price, the action
can report that it is unavailable while retaining the choice. Adding to Cart
does not mark the reminder handled or record a purchase.

The old Reminders page redirects to Alerts. New time rules, rule actions,
history, and personal-data export or reset use the unified workflow.
Use `/alerts/watch-rules/save` for rule edits and
`/alerts/product-events/{id}/read`, `/snooze` or `/add` for occurrence actions.
See the [test catalogue](Test/test-case-catalog.md) and
[dated test history](Test/history.md) for test commands and results.
