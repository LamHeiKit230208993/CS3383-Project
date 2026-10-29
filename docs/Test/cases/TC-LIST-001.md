# TC-LIST-001 — Saved list rules

Retailer-selection refinements expand this class to ten scenarios. Their final
execution result is recorded separately from the initial baseline run.

Source: [`ShoppingServiceTest`](../../../src/test/java/com/pricewatch/catalog/ShoppingServiceTest.java).

Add the same milk code with quantities 2 and 3. Expect one item with quantity 5,
the lowest known retailer and unit price, and a decimal line total. Update its
quantity and checked state; then remove it. Checking an item does not remove its
cost from the planned subtotal.

Reject zero, values above 999, and a combined quantity above 999. Combine known
and unknown-price items. Expect only known amounts in `knownSubtotal` and a
separate unknown-item count; a missing price is never zero.

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
