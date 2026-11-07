# TC-BUDGET-001 - Planned budget and category totals (J07)

Covers the planned weekly/monthly budget over the existing J04 list estimate and
the category breakdown. Estimates are never labelled actual spending.

Source: [`BudgetServiceTest`](../../../src/test/java/com/pricewatch/shopping/BudgetServiceTest.java).

Decision reference: D06 (whole-list estimate, warning boundary, category2 with
"Other" fallback, incomplete handling).

Steps and expectations:

1. Save a weekly budget. Add an item, then change its quantity. Expect the known
   subtotal grows. Change the retailer. Expect the estimate follows the new
   retailer's price.
2. Add an item with no current price. Expect `incomplete=true`,
   `unknownItemCount` includes it, and the known subtotal excludes it.
3. Add items across categories. Expect category amounts sum exactly to the known
   subtotal. Unmatched categories appear as "Other".
4. Set a low planned limit. Expect `overBudget=true` when the known subtotal
   exceeds it. Checked items keep their J04 subtotal contribution.

Limits: category level is `category2` (English label). Missing prices are never
counted as zero.
