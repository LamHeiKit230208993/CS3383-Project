# TC-REMINDER-001 · Legacy reminder upgrade

Run `LegacyReminderMigrationTest` with isolated H2, a checked sample
catalogue, and a clock fixed at 23 November 2026 at 16:30 HKT. Use tables
that match the previous schema. The workspace checked during planning had
0 rows in `replenishment_reminders` and 0 in `reminder_occurrences`.
The integration test uses populated fixtures.

1. Run the upgrade with both old tables empty, with both absent, and with
   a J05–J08 rule whose schema predates the pause switch and history.
   A rule without a saved pause value is enabled and keeps its due time and snooze.
2. Seed an enabled seven-day rule for two packs at AEON and a paused rule.
   Add an active snoozed occurrence and a dismissed occurrence whose old
   rule is gone. Run the upgrade twice. Check the rule count, intervals,
   due time, switch states, snooze, Cart defaults, occurrence times and
   dismissal. Confirm that the source tables remain available and version
   one personal-data export reads the migrated records.
3. Visit `/reminders` and check the redirect to Alerts. The retired write
   route returns 404. The Current rules tab and item editor show the
   migrated entry in the shared rule list.
4. Run two due checks on an enabled, overdue rule. Expect one occurrence.
   Add its saved two packs with AEON to Cart. Snooze and confirm Add is
   unavailable. Dismiss while snoozed and check that the due date moves
   seven days from now and that the occurrence remains in history.
5. Remove AEON's current price. The time event keeps the pinned retailer
   and disables Add. Edit the saved choice to JASONS and the quantity to
   three. Verify the pending occurrence reflects that change. Deleting the
   rule retains its dismissed occurrence.
6. Force the new history insert to fail with an H2 constraint. Verify that
   the rule transfer rolls back and both source rows survive. Remove the
   constraint and retry. Verify that personal-data reset clears migrated
   records and a later upgrade does not recreate them.

Source [LegacyReminderMigrationTest](../../../src/test/java/com/pricewatch/notifications/persistence/LegacyReminderMigrationTest.java).
