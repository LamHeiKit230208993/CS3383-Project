# TC-HISTORY-001 - Dated price history with gaps and empty state (J12, R10/R14)

Covers `/history`: only the dated records that exist are shown, missing days
are marked honestly, and an empty state appears before any observation.

Sources:
[`PersonalDataAndHistoryTest`](../../../src/test/java/com/pricewatch/web/PersonalDataAndHistoryTest.java),
[`HistoryService`](../../../src/main/java/com/pricewatch/web/HistoryService.java).

## Steps and expected results

1. With no tracking periods, open `/history`. The page shows the empty state
   **No priced records yet.** and no table.
2. Track a product (all retailers) but do not refresh live prices. The product
   appears with `0 records` per retailer and a note that no live observation is
   recorded yet.
3. Refresh live prices on day 1, then again four days later with a new price.
   `/history` lists two dated rows for that retailer (retrieval time, listed
   price, source) and one **No records** row between them naming the missing
   dates.
4. Consecutive same-day observations do not create a gap row.
5. A retailer with a missing price shows **Unknown** in the price column; the
   date row is still shown.
6. There is no weekly or monthly change summary and no invented daily series.

## Limits

- Times are when this app retrieved the data; the source does not supply a
  source price date. Week/month change (R14) remains unapproved and is not shown.
