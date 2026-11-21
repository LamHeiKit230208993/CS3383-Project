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
3. Confirm the app redirects to the Saved tracking page.
4. Inspect the demo catalogue and tracking periods.
5. Inspect history for milk, bread, and coffee, including the unknown coffee row.
6. Load a checked sample and submit demo data without confirmation.
7. Submit demo data again with the confirmation checkbox.

## Expected result

The demo load creates 3 demo products, 5 retailer periods, and 40 dated
observations. It starts product-wide tracking and creates synthetic target/drop
evidence. Demo rows show `demo://synthetic-price` as their source. Existing
catalogues are replaced only after server-side confirmation.

## Source

- [HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
- [DemoHistoryService](../../../src/main/java/com/pricewatch/tracking/DemoHistoryService.java)
- [PriceHistoryEntry](../../../src/main/java/com/pricewatch/tracking/PriceHistoryEntry.java)

## Limits

Demo values are synthetic and do not represent current shopping prices or live
provider availability.
