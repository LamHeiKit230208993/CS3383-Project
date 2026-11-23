# J05–J08 decision record

The [original issue #17 delivery plan](https://github.com/LamHeiKit230208993/CS3383-Project/blob/219cc5b62106c52562b8c2cc4ebead95c174c0cc/docs/Plan/tracking-alerts-budgets-reminders-j05-j08.md)
records the November 2026 milestone. The [historical implementation guide](https://github.com/LamHeiKit230208993/CS3383-Project/blob/d91efc0fcea038bfb35aa67ae38fedd3c9cdcebf/docs/implementation-j05-j08.md)
describes that release. Use the [current setup guide](../SETUP.md),
[Alerts implementation](../implementation-alerts.md), and
[test catalogue](../Test/test-case-catalog.md) for current work.

## Decisions before dependent work

D01–D07 identify the decisions reviewed during J05–J08. Their current
scope and successors are recorded here.

| ID | Current rule |
|---|---|
| D01 | Saving an exact pack in Wishlist starts all-retailer tracking, including retailers added on accepted live imports. A saved pack and a Cart choice are separate records. |
| D02 | Retain observations for 90 days. Ending a tracking period keeps its history. Restarting tracking makes a new period. |
| D03 | Store changed prices and unknown gaps from accepted live imports. Keep retrieval time and source provenance. A sample import creates no live observations. |
| D04 | Legacy targets rearm after a known price rises above their target. Watch rules also support a drop in HKD or percent using two accepted live prices for the same retailer and exact pack. |
| D05 | Use Hong Kong time. The active app checks due reminders and attempts a live refresh on first use each day. Quiet hours and scheduled summaries are retired. Earlier summary records stay exportable. |
| D06 | A weekly or monthly budget checks a planned Cart estimate. The Cart retains a pinned retailer when its price is missing. Checked items remain in the estimate. Category totals reconcile with the known subtotal. |
| D07 | Use time rules in Alerts for new reminders. Existing interval records keep their dates and Cart details through the upgrade. An Add to Cart action does not record a purchase. |
