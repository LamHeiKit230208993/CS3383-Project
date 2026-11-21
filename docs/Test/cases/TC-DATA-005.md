# TC-DATA-005 - Demo history data on the Price data page (J12)

Covers the **Load demo history data** panel on `/data`: one action seeds three
demo products, starts product-wide tracking, and records dated observations so
the History, Alerts, Reminders, and Tracking screens can be tested without
waiting for many real daily imports.

Sources:
[`DemoHistoryTest`](../../../src/test/java/com/pricewatch/web/DemoHistoryTest.java),
[`DemoHistoryService`](../../../src/main/java/com/pricewatch/tracking/DemoHistoryService.java).

## What it loads

| Product | Code | Retailers | Notable case |
|---|---|---|---|
| Demo Whole Milk 1L | `DEMO-MILK` | AEON, JASONS | Falls below a 9.00 target, rises back, falls again (target alert) |
| Demo Sandwich Bread 400g | `DEMO-BREAD` | AEON | Steady with one rise (no drop alert) |
| Demo Ground Coffee 200g | `DEMO-COFFEE` | WELLCOME, PARKNSHOP | One missing price (unknown row) |

Every retailer gets eight recorded days across the last three weeks; skipped
days become visible **No records** gap rows on `/history`.

## Steps and expected results

1. Open `/data`. A **Load demo history data** panel explains what it loads and
   warns that the prices are synthetic and replace the catalogue.
2. With no catalogue loaded, press **Load demo data**. The app redirects to
   `/history` with a notice naming the product and record counts.
3. `/history` shows the three demo products (with their codes), each retailer's
   8 dated rows, gap rows between the skipped days, and a `demo://synthetic-price`
   source label on every demo row.
4. The coffee retailer shows **Unknown** for the day with the missing price.
5. `/alerts` shows at least one target or drop event; `/saved` lists the demo
   products with tracking on; `/reminders` can create a reminder for them.
6. With a catalogue already loaded, the action needs the confirmation checkbox;
   without it the current catalogue is kept and an error notice appears.
7. Reloading the checked sample or live prices afterwards replaces the demo data.

## Limits

- The demo prices are synthetic and must not be read as real prices.
- Loading replaces the accepted catalogue, like the sample and live actions.
