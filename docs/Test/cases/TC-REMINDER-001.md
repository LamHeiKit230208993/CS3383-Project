# TC-REMINDER-001 · Time rules and alert history

Run `ReminderControllerTest` and `ReminderPersistenceRestartTest` with the
checked sample catalogue, isolated H2 databases, and a controlled clock.

1. Open an old reminders link with and without a product code. Verify that
   it reaches the item rule panel or Alerts. Create a 7-day rule for three
   packs at JASONS. Check the saved fields and first due time.
2. Edit the quantity and retailer. Check that the due time stays the same.
   Change the interval and check that the next due time starts from the edit.
3. Advance the clock to the due time. Run two due checks at once and verify
   that they record one active occurrence. Delete the rule and check that
   the same occurrence remains in history without active actions.
4. Snooze a due occurrence for 24 hours. Verify that it becomes unavailable
   and returns with the same ID after the snooze. Clear it and advance to
   the next due time. Verify that the first occurrence remains in history
   and a new occurrence appears. An action on the first occurrence leaves
   the new one alone. A shopper can also clear a snoozed occurrence early.
5. Add from a due occurrence with a retailer conflict in Cart. Verify that
   the action fails without changing the Cart or reminder. Resolve the
   conflict and retry. Verify that the saved quantity and retailer are added
   and the occurrence stays active. Check invalid quantities and intervals,
   capacity failures, and missing prices on a pinned retailer.
6. Save a rule to a temporary H2 file and remove the empty occurrence
   table to represent the previous schema. Reopen it when the rule is due.
   Record and snooze its occurrence. Reopen the app again and verify that
   the rule, occurrence ID, and snoozed due time survive.

The tests advance the application clock and call the due check directly.
They exercise in-app alert history and Cart, including the legacy form routes.

Sources [ReminderControllerTest](../../../src/test/java/com/pricewatch/reminder/ReminderControllerTest.java),
[ReminderPersistenceRestartTest](../../../src/test/java/com/pricewatch/reminder/ReminderPersistenceRestartTest.java).
