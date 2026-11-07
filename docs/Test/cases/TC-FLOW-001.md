# TC-FLOW-001 - Combined J05-J08 flow

Covers the issue's combined flow: a tracked price update creates the expected
alert, updates the list estimate/budget result, and a reminder adds the intended
item.

Source: [`CombinedFlowTest`](../../../src/test/java/com/pricewatch/CombinedFlowTest.java).

Steps and expectations:

1. Import 9.00; save the product. Expect it is saved but not tracked.
2. Enable tracking, add a DROP rule, set a weekly budget, add 2 packs to My list.
3. Re-import 9.00 as the tracked baseline. Expect no event yet.
4. Import 7.50. Expect one drop event and the budget known subtotal falls.
5. Create a reminder (interval 7 days), advance the clock 7 days, add to list.
   Expect the reminder's quantity is added to My list.

Limits: the test uses a controlled clock and constructed imports. Each feature is
also tested separately (TC-TRACK, TC-ALERT, TC-BUDGET, TC-REMINDER).
