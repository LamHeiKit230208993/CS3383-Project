# TC-WEB-003 — Saved list page: empty state, subtotal, remove control

| Field | Details |
|---|---|
| Test class | `PageControllerTest` |
| Test methods | `listPageStartsEmpty()`, `listRowShowsSubtotalForTheQuantity()`, `listRowHasARemoveControl()` |
| Type | Full context test rendering the real Thymeleaf template |
| Request | `GET /list` |
| Setup | The list store is reset before each request |

## Purpose

Verify that an untouched app shows no invented items, that a row shows the
cost for its quantity, and that every row offers a way to remove it.

## Steps

1. Reset the list to empty, then `GET /list`.
2. Add 2 packs of `P000000124` (AEON HK$7.50 each), then `GET /list`.
3. Add 1 pack of `P000000229`, then `GET /list`.

## Expected result

- With an empty list, the page renders `Your list is empty` and a link back
  to search, and shows no total.
- The 2-pack row shows the per-pack amount `HK$7.50` and the subtotal
  `HK$15.00`.
- The row form posts to `/list/items/P000000229/remove` and its button is
  labelled `Remove Crustless Sandwich Bread 6 slices from the list`.

## Limits

This case checks the rendered page only. Remove behaviour itself, and the
subtotal changing with quantity, are covered by
[TC-LIST-001](./TC-LIST-001.md) and the browser run recorded in the catalog.

Source: [`PageControllerTest.java`](../../../src/test/java/com/pricewatch/web/PageControllerTest.java)
