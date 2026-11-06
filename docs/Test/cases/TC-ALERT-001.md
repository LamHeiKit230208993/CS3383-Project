# TC-ALERT-001 — Price alerts and local notification timing

## Purpose

Check target-price and price-drop rules, duplicate prevention, quiet hours,
snooze/read persistence, and the once-per-day in-app summary.

## Setup

- Start the Spring test application with an isolated H2 database.
- Use constructed `LIVE` price observations for one exact product and retailer.
- Supply explicit instants for quiet-hour and summary boundary checks.

## Steps

1. Track a product after a `LIVE` catalogue is accepted. Set a target equal to
   or above its current known price. Confirm one target event can be created
   from the current accepted LIVE listing. Confirm a SAMPLE or stale LIVE
   import cannot create one. A current unchanged LIVE price may qualify after
   its old history row expires, without restoring that row.
2. Set the same target again and import an unchanged price. Confirm no duplicate
   target event is created.
3. Import a price above the target, then a price at or below it. Confirm the
   above-target observation rearms the target rule and the later observation
   creates one new event.
4. Enable the drop rule. Import a lower known price after a known observation.
   Confirm one drop event is created. Repeat the same price and confirm no
   duplicate event is added. Confirm the alert shows both prices, both retrieval
   times, and a separate generation time.
5. Add an unknown-price gap between known observations. Confirm the next known
   observation is not compared across the gap.
6. Check the Hong Kong quiet-hour start and end instants. Confirm alert
   availability is delayed through quiet hours, then allowed after the end.
7. Snooze an alert. Confirm its due time is stored for 24 hours. Mark it read and
   confirm read state remains stored.
8. Open the page before the snooze ends or during quiet hours. Confirm the page
   shows only a generic waiting count, with no pending product, retailer,
   message, price, or action. Confirm the details appear when the event becomes
   available.
9. Process a morning summary twice for the same Hong Kong local date. Confirm a
   single summary is stored with its actual generation time. Read and snoozed
   events must not enter it. Set the summary to 23:00 during 22:00–08:00 quiet
   hours; confirm it becomes eligible at 08:00 the next day. Run concurrent
   checks and confirm only one summary is stored.
10. Reopen/read the stored alert and timing settings. Confirm they remain saved
   in the local H2 database.

## Expected result

Target alerts use a `<=` boundary and repeat only after a later known price above
the target rearms the rule. Drop alerts compare only adjacent known observations
in the same tracking period and retain evidence for the comparison. Quiet hours
and snoozes delay availability without losing event state. Read and not-yet-
available snoozed events are not counted. A daily summary is stored at most once
per Hong Kong date, including when its configured time falls in quiet hours.

## Source

- [AlertServiceTest](../../../src/test/java/com/pricewatch/notifications/AlertServiceTest.java)
- [AlertService](../../../src/main/java/com/pricewatch/notifications/AlertService.java)
- [NotificationPolicy](../../../src/main/java/com/pricewatch/notifications/NotificationPolicy.java)

## Limits and review notes

Tests use local records and explicit time values. They do not prove device or
cloud notification delivery, background collection while the app is closed, or
current prices. The app has no accounts or cloud storage.
