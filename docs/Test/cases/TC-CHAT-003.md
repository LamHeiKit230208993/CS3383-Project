# TC-CHAT-003 · Ask shopping tools

Use the exact-pack and named-list fixtures in
[AskShoppingToolsTest](../../../src/test/java/com/pricewatch/web/AskShoppingToolsTest.java).
The test doubles cover tool validation and proposed writes without a provider
connection. Browser and controller behavior are covered by
[TC-CHAT-002](TC-CHAT-002.md).

1. Prepare the same named-list add twice in one turn. Apply the resulting
   operation once. Expect one request to add the requested number of packs to
   that list and keep its retailer.
2. Prepare a one-pack removal. Change the row quantity before applying.
   Expect a fresh preview. Applying that preview reduces the quantity by one
   and keeps the selected retailer.
3. Prepare a combined price and calendar rule over two turns. Check that the
   draft identifies missing fields and the complete preview shows Hong Kong
   time. Expect no saved rule before applying the proposal.
4. Read a named list with one unknown price in Traditional Chinese. Expect the
   known subtotal, incomplete estimate, saved limit, and an unknown-price label.
5. Calculate grouped amounts and a percentage. Expect exact decimal results
   for simple operations, a currency result in HKD, and an error for division
   by zero or unsupported syntax.
