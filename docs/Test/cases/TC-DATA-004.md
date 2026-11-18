# TC-DATA-004 - View, export, and delete local personal data (J12, R45)

Covers `/my-data`, the JSON export, and the confirmed delete. The accepted
catalogue is source data and is never treated as the user's personal data.

Sources:
[`PersonalDataAndHistoryTest`](../../../src/test/java/com/pricewatch/web/PersonalDataAndHistoryTest.java),
[`PersonalDataService`](../../../src/main/java/com/pricewatch/web/PersonalDataService.java).

## What counts as personal data

- My list items (product, quantity, checked, chosen retailer)
- Wishlist entries
- Price tracking periods and their price observations
- Alert events and the alert-timing settings
- Replenishment reminders
- Budget settings

The catalogue (products, listings, offers) and the import metadata are source
data, not the user's, and are kept on delete. List sharing (J10) exports only
the chosen list, not all of this.

## Steps and expected results

1. Add a list item, save a wishlist entry, start tracking, and set a reminder.
2. Open `/my-data` (**Settings**). The page shows the language switch, the
   contrast switch, then each data area with the matching row count; the user
   data table fills its panel without clipping or collapsing into a narrow
   column; the two settings rows show **Saved** or **Defaults**.
3. Open `/my-data/export`. The browser downloads `market-desk-my-data.json`
   (`application/json`) with `listItems`, `wishlist`, `tracking` (with
   `observations`), amounts as exact decimal strings, and the note that the
   catalogue is excluded.
4. Submit the delete form with the wrong word. Nothing is removed and an error
   notice appears.
5. Submit the delete form with `DELETE`. Every user row is removed, the catalogue
   and its import metadata remain, and the page shows zero counts.

## Limits

- Delete cannot remove copies already shared with another device.
