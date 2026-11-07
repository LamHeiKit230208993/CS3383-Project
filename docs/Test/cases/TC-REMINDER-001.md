# TC-REMINDER-001 - User-set replenishment reminders (J08)

Covers user-set intervals, due evaluation on a controlled clock, add-to-list
reusing the shopping rules, clear, and snooze.

Source: [`ReminderServiceTest`](../../../src/test/java/com/pricewatch/reminder/ReminderServiceTest.java).

Decision reference: D07 (interval in days, initial due, clear vs snooze, next
date, add-to-list effect).

Steps and expectations:

1. Create a reminder (interval 7 days). Expect it is not due; product and
   quantity are stored. Advance 7 days. Expect it appears in `dueReminders`.
2. Add to My list. Expect the quantity is added via `ShoppingService.add`, and the
   reminder is rescheduled to now + interval.
3. A quantity of 999 plus another add triggers the existing 1-999 combined limit.
   An existing item with a different retailer triggers the existing conflict
   error (reuses `ShoppingService.add`, not re-implemented).
4. Snooze. Expect the due time moves to now + 24h. Clear. Expect removal.
5. Interval below 1 day is rejected.

Limits: reminders run only while the app runs; evaluation happens on page load.
A reminder action does not prove a purchase.
