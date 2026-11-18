# TC-SHARE-001 · Editable list sharing

Use isolated local app instances. Load the controlled catalogue in the first
instance. Save two products, pin one retailer and check one item. Export the
selected list. Upload the file on the second instance, where the catalogue has
no prices. Check that both rows are present, the pinned retailer and checked
state survive, and prices show **Unknown**. Edit the imported quantity and
check that the sender's list keeps its original quantity.

Upload malformed JSON. Check that the error is visible and the list picker can
still reopen the edited copy. The service checks also cover receiving-catalogue
prices and rejected file content without partial saves.

This check transfers a file between independent databases on one machine.
It does not require a cloud service or synchronized edits.

Sources: [browser suite](../../../src/test/browser/market-desk.cjs),
[ShoppingListTransferTest](../../../src/test/java/com/pricewatch/shopping/ShoppingListTransferTest.java).
