# TC-OFFLINE-001 · Use a downloaded checklist offline

1. Use an isolated app and controlled live price fixture. Save a weekly budget
   of HK$100, import a list, and choose **List actions → Download offline checklist**.
2. Stop that app, disconnect the Chrome context from the network, and open
   the file by its local path. Verify quantities, checked states, retailer
   choices, estimates, retrieval time, and starting budget. Check that the
   file has no app sidebar and makes zero HTTP requests.
3. Use the keyboard to check another row. Check the completion count and
   verify that checked lines lock the retailer and pack fields. Reopen the
   milk row, choose Wellcome, and set three packs. The line total is HK$36.
4. Enter zero packs. The row becomes Unknown and the list shows a known
   subtotal. Restore three packs and lower the budget to HK$1. The progress
   bar fills and the over-budget amount appears. Clear the budget to remove
   the limit. Check horizontal fit at 320, 375, and 1280 px.
5. The HTTP tests check the initial estimate and budget, a missing retrieval
   time, safe HTML escaping, and redirects for the old offline routes.

Sources [browser suite](../../../src/test/browser/market-desk.cjs) and
[HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java).
