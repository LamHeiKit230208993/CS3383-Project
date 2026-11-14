# TC-BUDGET-001 — Planned budget and list estimate

This case covers saved weekly/monthly limits, effective list estimates, category
totals, incomplete prices, and native form validation. A budget is a plan for the
current list. It is not a spending ledger, period accumulator, or rollover.

Sources: [`BudgetServiceTest`](../../../src/test/java/com/pricewatch/budget/BudgetServiceTest.java),
[`BudgetControllerTest`](../../../src/test/java/com/pricewatch/budget/BudgetControllerTest.java),
[`BudgetPageTest`](../../../src/test/java/com/pricewatch/budget/BudgetPageTest.java), and
[`BudgetSettingsPersistenceTest`](../../../src/test/java/com/pricewatch/budget/BudgetSettingsPersistenceTest.java).

1. Open `/budget` with no catalogue or list items. Expect a weekly period
   selected by default, no limit saved, and a clear empty state.
2. Save a valid weekly or monthly nonnegative HKD limit. Expect a redirect and a
   confirmation. Reopen the page and expect the period and exact decimal limit
   to remain saved.
3. Compare the current list's known subtotal with the limit. The subtotal must
   use the effective line totals from My list, including checked items. An
   explicitly selected JASONS milk listing at HK$12.50 must remain HK$12.50 per
   pack when AEON lists HK$7.50; multiplying quantity 2 gives HK$25.00. The
   budget calculation must not choose a cheaper retailer.
4. Group known line totals by the English level 2 category. Missing or blank
   categories use `Uncategorised`. Category amounts must add up to the known
   subtotal; unknown-price items add no invented amount.
5. If any price is unknown and the known subtotal is at or below the limit,
   report that the full total is unknown. If the known subtotal already exceeds
   the limit, keep the over-limit warning even when other prices are unknown.
   Do not show a within-limit result for an incomplete total.
   An over-limit list with no unknown prices must not claim that more prices are
   unknown. At the exact limit, report the estimate as within budget.
6. Reject an invalid period, blank or malformed amount, negative amount, more
   than four fractional digits that require rounding, or a value outside
   DECIMAL(19,4). Expect no settings write. A valid value with removable
   trailing zeroes is accepted without changing its value.
7. Close the application context after saving settings to a temporary H2 file.
   Open a second context against the same file. Expect the same period and exact
   decimal limit in its single settings row. The test uses a temporary database,
   not the user's production data file.

The weekly/monthly choice labels the planned limit only. The app does not infer
past spending, roll unused amounts forward, include offers, or add delivery and
stock costs.
