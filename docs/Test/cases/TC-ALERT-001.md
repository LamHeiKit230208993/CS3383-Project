# TC-ALERT-001 - In-app target and price-drop alerts (J06)

Covers target and drop evaluation, duplicate prevention, quiet hours, snooze,
and the morning summary.

Source: [`AlertServiceTest`](../../../src/test/java/com/pricewatch/alerts/AlertServiceTest.java).

Decision references: D03 (unknown price excluded), D04 (target boundary and
earlier comparison), D05 (time, quiet hours, snooze, summary).

Steps and expectations:

1. Track a product and add a TARGET rule (e.g. 8.00). Import a known price of
   7.50. Expect one event of type TARGET at 7.50 (at or below fires).
2. Add a DROP rule. Import 9.00 (no earlier price) → no event. Import 8.00 →
   one drop event with `comparedPrice` 9.00. Import 8.00 again → no duplicate.
3. Import a null price → no event (unknown never fires).
4. Set the clock into quiet hours (22:00-07:00 HKT). Expect the event is held
   (`heldEvents` non-empty, `showHeldNow` returns 0, not visible). Advance to
   after 07:00. Expect the event is shown and appears in `morningSummary`.
5. Snooze an event. Expect it is hidden for 24 hours, then visible again.

Limits: the controlled `Clock` is a test bean; the running app uses the system
clock in Asia/Hong_Kong. A closed app delivers nothing (evaluation only runs on
page load/refresh).
