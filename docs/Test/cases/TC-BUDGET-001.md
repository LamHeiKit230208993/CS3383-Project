# TC-BUDGET-001 — Planned budget and category totals

This case covers J07 planned budgets and category totals on My list. Dated
execution results are recorded separately in the test history.

Source: [`BudgetServiceTest`](../../../src/test/java/com/pricewatch/budget/BudgetServiceTest.java).

Set a weekly or monthly budget (`POST /budget`) with a positive limit. The list
page compares the **known subtotal** to the limit and warns when exceeded (D06).
The warning is a plan, never labelled actual spending.

Category totals use `category2` → fallback `category1` → "Uncategorised". The sum
of category known amounts must reconcile with the known subtotal because both come
from the same list rows. Quantities and retailer choices change the estimate.

Unknown prices keep the estimate incomplete: an item with no current price adds to
the unknown count and is excluded from known amounts (missing is never zero). The
historical-sample regression: JASONS milk stays HK$12.50 per pack even when AEON
lists HK$7.50, because the explicit choice is used. Checked items keep J04 total
behaviour. Clearing the budget (`POST /budget/clear`) removes the setting. There is
no rollover; the estimate is a live snapshot of the planned list.
