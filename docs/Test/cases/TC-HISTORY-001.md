# TC-HISTORY-001 · View recorded prices for an exact pack

Use an isolated H2 database and a controlled price source. Start tracking a
product at one retailer. Accept a known price and a later missing price, then
open **Price history** from Wishlist or that product's comparison. The latest row shows
the retailer, its retrieval time, and **Unknown**. The earlier row shows the
previous price and time. An untracked pack shows an empty state.

Select a retailer and the 30-day range. Confirm the chart and the dated table
show only that selection. Check that rows with unknown prices stay in the table.
Each retailer has a labeled line style. A gap or a new tracking period starts
a separate line. A single known price shows as a point.

The tracking test places a record outside the 90-day window and verifies that the
table excludes it without writing to the database. Chrome checks the same pack
as Ask and list sharing, with localized headings and a 375 px viewport.
The fixture price fits on one line at that width.

Sources [PriceHistoryChartTest.java](../../../src/test/java/com/pricewatch/web/PriceHistoryChartTest.java),
[TrackingServiceTest.java](../../../src/test/java/com/pricewatch/catalog/TrackingServiceTest.java), and
[market-desk.cjs](../../../src/test/browser/market-desk.cjs).
