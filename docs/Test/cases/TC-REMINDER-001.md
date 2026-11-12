# TC-REMINDER-001 — Replenishment reminders

This case covers J08 reminders on the dedicated `/reminders` page. Dated
execution results are recorded separately in the test history.

Source: [`ReminderServiceTest`](../../../src/test/java/com/pricewatch/reminders/ReminderServiceTest.java).

Create a reminder with a product code, quantity (1–999), an interval in days, and
a snooze in days. Initial next due = creation time + interval (D07). Due state is
computed from the clock on page load; reminders run only while the app is open.

Add-to-list reuses J04's `ShoppingService.add` rules, including the quantity limit
and the existing-item/other-retailer conflict; it does **not** change reminder
state. Clear deactivates the reminder. Snooze adds the snooze interval to the next
due date. Product and quantity survive actions and restart. Unavailable prices
remain unknown.

Invalid interval, snooze, quantity, or a missing product are rejected.
