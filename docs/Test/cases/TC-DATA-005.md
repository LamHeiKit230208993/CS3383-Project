# TC-DATA-005 - Synthetic demo tracking history

## Purpose

Check that the Price data screen can seed clearly labelled synthetic tracking
history for demonstrating tracking and alerts.

## Setup

- Start the Spring test application with an isolated H2 database.
- Do not contact the Consumer Council feed.

## Steps

1. Open Settings > Price data or `/settings`.
2. Submit **Load demo data** with no catalogue loaded.
3. Follow the redirect to Wishlist. Check that all three demo products and
   the milk history link are visible immediately.
4. Inspect the demo catalogue and tracking periods.
5. Inspect history for milk, bread, and coffee, including the unknown coffee row.
6. With a fixed clock, check the order of AEON milk prices and its saved
   latest observation. Advance the clock one hour and load Demo again. Expect
   the same 40 observations, 2 alerts, 5 tracking periods, and 3 Wishlist
   entries. The saved observation timestamps are unchanged.
7. Load a checked sample and submit demo data without confirmation.
8. Submit demo data again with the confirmation checkbox.

## Expected result

The demo load creates 3 demo products, 5 retailer periods, and 40 dated
observations. The AEON milk series starts at HK$10.00 21 days ago and ends at
HK$8.90 one day ago. Alerts evaluate each price against the preceding
observation. The last price and time agree with the latest history row.
Demo rows show `demo://synthetic-price` as their source. Existing catalogues
are replaced only after server-side confirmation.

## Source

- [HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
- [TrackingServiceTest](../../../src/test/java/com/pricewatch/catalog/TrackingServiceTest.java)
- [DemoHistoryService](../../../src/main/java/com/pricewatch/tracking/DemoHistoryService.java)
- [PriceHistoryEntry](../../../src/main/java/com/pricewatch/tracking/PriceHistoryEntry.java)

## Limits

Demo values are synthetic and do not represent current shopping prices or live
provider availability.
Reloading keeps saved demo records. Duplicate records already saved by older
builds stay in history.
