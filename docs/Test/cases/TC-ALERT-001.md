# TC-ALERT-001 — Existing retailer price rules

## Purpose

Check retailer-specific target and drop rules, duplicate prevention, and saved
alert state after the timing change.

## Setup

- Start the Spring test application with an isolated H2 database.
- Use constructed `LIVE` price observations for one exact product and retailer.
- Supply explicit instants for the 24-hour snooze boundary and older timing records.

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
6. Configure historical quiet hours from 22:00 to 08:00 Hong Kong time.
   Trigger a price alert at 23:00 and confirm it is available immediately.
   Insert a second event with a saved delay until 08:00. Confirm it is
   also available immediately and retains its original stored timestamp.
7. Snooze a price alert. Confirm the event becomes available at exactly 24 hours
   and keeps its recorded amount. Mark an event read and confirm its state is saved.
8. Seed an old summary, read alert history the next day, and confirm the
   prior summary is retained and no new summary is written.
9. Reopen the H2 database. Check the alert evidence, read and snooze state,
   prior summary, and historical timing preferences. Reimport the same prices
   and confirm there are no repeated alerts.
10. Switch off a retailer rule with a target and drop condition. Accept a lower
    price. Verify that price history updates and no alert fires. Remove the item
    from Wishlist and re-add it. The switch stays off. Turn it on, then accept
    qualifying prices and verify that alerts resume.
11. Delete both conditions. Verify that tracking and earlier history stay
    intact. Reopen an older database without a switch column and verify its
    existing rules start enabled. Switch a rule off and verify that it stays
    off after another restart.

## Expected result

Target alerts use a `<=` boundary and repeat only after a later known price above
the target rearms the rule. Drop alerts compare only adjacent known observations
in the same tracking period and retain evidence for the comparison. Snooze
controls when a price alert becomes available. Old summary and timing records
remain in local storage for personal-data export.

## Source

- [AlertServiceTest](../../../src/test/java/com/pricewatch/notifications/AlertServiceTest.java)
- [TrackingPersistenceRestartTest](../../../src/test/java/com/pricewatch/catalog/TrackingPersistenceRestartTest.java)
- [AlertService](../../../src/main/java/com/pricewatch/notifications/AlertService.java)
- [NotificationPolicy](../../../src/main/java/com/pricewatch/notifications/NotificationPolicy.java)

## Limits and review notes

Tests use local records and explicit time values for in-app alerts.
