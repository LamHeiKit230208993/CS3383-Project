# TC-OFFLINE-001 · Use a downloaded checklist offline

1. Use an isolated app and controlled live price fixture. Import a list and
   choose **List actions → Download offline checklist**.
2. Stop that app, disconnect the Chrome context from the network, and open
   the file by its local path. Verify quantities, checked states, retailer
   choices, estimates, and retrieval time against the Cart. Check that the
   file makes zero HTTP requests.
3. Use the keyboard to check another row. Its name is crossed out and its
   price remains visible. The browser test checks the mobile layout.
4. The HTTP tests check an empty cart, a missing retrieval time, safe HTML
   escaping, and redirects for the old offline routes.

Sources [browser suite](../../../src/test/browser/market-desk.cjs) and
[HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java).
