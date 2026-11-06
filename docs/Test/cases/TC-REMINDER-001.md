# TC-REMINDER-001 — Local replenishment reminders

## Purpose

Check that a reminder keeps its product, quantity, interval, and optional
retailer choice; computes due dates from the controlled clock; respects shared
quiet hours for the due indicator; and uses the saved-list rules when adding
packs to My list.

## Setup

- Focused command (from the project root):
  `.\mvnw.cmd -q "-Dtest=ReminderControllerTest,ReminderPersistenceRestartTest" test`.
- Run `ReminderControllerTest` and `ReminderPersistenceRestartTest` with the
  project test dependencies.
- Use the isolated H2 database configured by that test and the checked sample
  catalogue.
- Set the application clock to `2026-11-06T02:00:00Z` unless a step advances it.
- Stub the shared notification policy for the quiet-hours scenarios.
- The reminder screen checks due state when it is opened. It does not deliver
  reminders while the application is closed.

## Steps and expected results

1. Open `/reminders?code=P000000124` and save 3 packs for 7 days with retailer
   JASONS. The page offers labelled quantity and interval fields and an optional
   retailer choice. The saved record retains those values and has its first due
   time at creation plus 7 days.
2. Advance the clock to the due time and snooze. The current due time moves
   forward by 24 hours. The regular interval remains 7 days. Clear at the
   snoozed due time; the next due time becomes clear time plus 7 days and the
   snooze is removed.
3. While the shared policy reports quiet hours, open the page at a due time. The
   per-reminder due indicator is hidden. Manual clear and snooze forms still
   work because the user explicitly opened the app.
4. Add a reminder to My list when the product already exists with another
   retailer. The action shows the existing retailer-conflict error and changes
   neither the list nor the reminder. Remove the conflict and retry. The chosen
   quantity and retailer are added; the reminder remains active and due.
5. Create a reminder pinned to JASONS, then remove current listings. The page
   reports that the price is unknown and preserves JASONS. Adding to My list
   fails under the existing price validation and does not change either record.
6. Check create validation. Quantities below 1 or above 999 and intervals below
   1 are rejected without saving a reminder. A shopping-list total above 999 is
   rejected without clearing the reminder or changing the existing item.
7. Start the app with a temporary file-backed H2 database, save and snooze a
   reminder, close that Spring Boot context, and open a new context against the
   same database. The product, quantity, interval, retailer choice, and snoozed
   due time remain available.

## Automated source

`src/test/java/com/pricewatch/reminder/ReminderControllerTest.java` and
`src/test/java/com/pricewatch/reminder/ReminderPersistenceRestartTest.java`

## Limits and review notes

The restart test closes and reopens Spring Boot contexts over a temporary file
database. Due state is calculated when the reminders page is opened; the test
does not claim a notification was delivered while the app was closed.
