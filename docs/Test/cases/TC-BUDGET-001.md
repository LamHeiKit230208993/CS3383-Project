# TC-BUDGET-001 — Planned budget and list estimate

This case covers saved weekly/monthly limits, effective list estimates, category
totals, incomplete prices, and native form validation. A budget is a plan for the
current list. It is not a spending ledger, period accumulator, or rollover.

Sources: [`BudgetServiceTest`](../../../src/test/java/com/pricewatch/budget/BudgetServiceTest.java),
[`BudgetPageTest`](../../../src/test/java/com/pricewatch/budget/BudgetPageTest.java), and
[`BudgetSettingsPersistenceTest`](../../../src/test/java/com/pricewatch/budget/BudgetSettingsPersistenceTest.java).

## Targeted decision coverage

The first guard in [`BudgetView.progressPercent()`](../../../src/main/java/com/pricewatch/budget/BudgetView.java)
returns zero when **A** (there is no saved limit) or **B** (the subtotal is
zero) is true.

| Case | Limit | Subtotal | A | B input | Guard | Percentage |
|---|---:|---:|---|---|---|---:|
| M1 | HK$100 | HK$25 | False | False | False | 25 |
| M2 | None | HK$25 | True | False | True | 0 |
| M3 | HK$100 | HK$0 | False | True | True | 0 |

Use M1 with M2 for A and M1 with M3 for B. Each pair changes one condition's
input and changes the guard outcome. In M2, Java short-circuits B. Its input
is still HK$25, while the actual false and true evaluations of B occur in M1
and M3. The three cases are regression tests in
[`BudgetViewTest`](../../../src/test/java/com/pricewatch/budget/BudgetViewTest.java).
The test also checks the balance when the limit is absent or present.

The additional warning in the [budget view](../../../src/main/resources/templates/pages/personal.html)
requires **A** (known subtotal above the limit) and **B** (at least one unknown
price). Use the checked milk and bread sample. The milk price is HK$7.50.
Switch the pinned bread retailer's price to unknown when the case calls for it.

| Case | Limit | Bread price | A | B input | Additional warning |
|---|---:|---|---|---|---|
| W1 | HK$0 | Unknown | True | True | Shown |
| W2 | HK$0 | HK$11.50 | True | False | Hidden |
| W3 | HK$100 | Unknown | False | True | Hidden |

W1 and W3 exercise A independently. W1 and W2 exercise B independently.
The template skips B when A is false. MockMvc renders the actual template in
each case. The analysis applies to these two selected decisions.

## Maintained budget cases

1. Open **Settings → Personal data** with no catalogue or Cart items. Expect a
   weekly period selected by default and no limit saved. Old `/budget` links
   redirect to the Budget card. Legacy form submissions still save there.
2. Save a valid weekly or monthly nonnegative HKD limit. Expect a redirect and a
   confirmation. Reopen the page and expect the period and exact decimal limit
   to remain saved.
   From unified Settings, success or a validation error returns to the Budget
   anchor. A rejected edit preserves the stored limit.
3. Compare the current list's known subtotal with the limit. The subtotal must
   use the effective line totals from My list, including checked items. An
   explicitly selected JASONS milk listing at HK$12.50 must remain HK$12.50 per
   pack when AEON lists HK$7.50; multiplying quantity 2 gives HK$25.00. The
   budget calculation must not choose a cheaper retailer.
4. In the budget service, group known line totals by the English level 2 category. Missing or blank
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
8. Apply a saved limit to the totals supplied by Discover or Cart. For HK$78.00
   of a HK$200.00 limit, expect 39% and HK$122.00 available. Confirm incomplete
   prices do not produce a within-budget state. Check a zero limit with a known
   subtotal and an empty total to cover both division-by-zero cases.

The weekly/monthly choice labels the planned limit only. The app does not infer
past spending, roll unused amounts forward, include offers, or add delivery and
stock costs.
