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
6. Export Cart copies from the historical sample and Demo catalogues. After
   stopping the app, open both copies offline. Each header shows its source
   label and retrieval timestamp. Check that the saved AEON amounts remain
   HK$7.50 in the sample and HK$10.00 in Demo. The HTTP regression also
   checks that the Demo export uses **Synthetic demo catalogue**.

## Optional JavaScript coverage

Set `BROWSER_JS_COVERAGE=1` before running the Chrome shopper suite. A
successful run writes `target/browser-checks/javascript-coverage.json` and
requires data for Cart, Ask, and the offline checklist. The offline count
uses the exact inline JavaScript from the downloaded file, including only
source units with nonzero V8 execution ranges. The surrounding HTML and CSP
nonce are outside the measured source.

Use `node --test src/test/browser/javascript-coverage.test.cjs` to verify that
nested zero-count blocks are respected before merging source ranges from
separate executions. The test also checks that missing script data fails the
report. These figures count UTF-16 source units and do not measure
JavaScript branch coverage or branch independence. Chrome can discard
earlier execution data when it changes renderers during navigation. The
suite collects the local file separately from the app pages to cover its
script after that transition.

Sources [browser suite](../../../src/test/browser/market-desk.cjs) and
[HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java).
