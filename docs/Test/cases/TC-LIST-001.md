# TC-LIST-001 — Saved list rules

This case covers saved-list and retailer-selection rules. Dated execution
results are recorded separately in the test history.

Source: [`ShoppingServiceTest`](../../../src/test/java/com/pricewatch/catalog/ShoppingServiceTest.java).

Add the same milk code with quantities 2 and 3. Expect one item with quantity 5,
the lowest known retailer and unit price, and a decimal line total. Update its
quantity and checked state, reopen it, then remove it. Checking an item does not remove its
cost from the planned subtotal.

For issue #31, use the checked sample and mark one AEON milk pack Done.
The service rejects adds, reminder adds, details edits, legacy updates, and
row removal while the item is Done. Verify the count, retailer, checked
state, and HK$7.50 subtotal stay the same. Reopen the row with the Done
control and verify editing and removal work again.
[HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
checks the Cart error and reopen path.
[AskShoppingToolsTest](../../../src/test/java/com/pricewatch/web/AskShoppingToolsTest.java)
checks a pending removal after the row becomes Done and rejects a new proposal
while the row is locked.
The [Chrome shopper suite](../../../src/test/browser/market-desk.cjs) opens
Cart in two tabs, checks off the row in one tab, and attempts to increase
the quantity from the stale tab. The app shows a reopen message, keeps the
original quantity, and leaves Done selected.

Reject zero, values above 999, and a combined quantity above 999. Combine known
and unknown-price items. Expect only known amounts in `knownSubtotal` and a
separate unknown-item count; a missing price is never zero.

Check both quantity limits against the guard **A or B**, where **A** is
quantity below 1 and **B** is quantity above 999.

| Quantity | A | B input | Expected result |
|---:|---|---|---|
| 0 | True | False | Reject the edit |
| 1 | False | False | Accept the minimum |
| 999 | False | False | Accept the maximum |
| 1,000 | False | True | Reject the edit |

The 0 and 1 pair exercises A independently. The 999 and 1,000 pair exercises
B independently. Java skips B for 0. The test saves 999 checked packs with
JASONS selected, attempts to change quantity, checked state, and retailer
using each invalid value, then checks that the saved row is unchanged.

Import a replacement without a saved product. Expect the item and checked state
to remain, the product to be inactive, and its current price to be unknown.
Reject an invalid import after saving a list. Expect unchanged accepted metadata
and list values. Database reopen is checked in TC-DATA-001.

A concurrency case submits adds from 12 threads to an initially empty My list.
Expected: one list, one product item, and the full accumulated quantity without
lost updates. This checks the local single-app mutation rule; it is not a
multi-server or cloud collaboration guarantee.

Retailer-choice scenarios check that same-choice adds combine quantities, while
different-choice adds direct the user to change the item in My list. An explicit
known-price retailer changes effective unit price and line total. Blank selection
restores automatic lowest. The legacy update retains a saved choice. Unknown or
unpriced new retailer choices are rejected. After refresh removes the selected
retailer's price, its selection remains saved and its total stays unknown rather
than using another retailer. Offers are not included in the arithmetic.

In the [shopper browser suite](../../../src/test/browser/market-desk.cjs),
open a named cart with enough items to scroll. At 1280 × 650, the summary
stays in view as the list scrolls and the expanded import button remains
reachable by focus. At 375 px, it follows the items in normal page flow.
From a Cart item comparison, verify that the back arrow reveals the destination
name on hover and keyboard focus. Enter returns to the Cart that opened it.
