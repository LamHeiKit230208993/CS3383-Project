# TC-OFFLINE-001 · Review a saved list offline

Use an isolated app and the controlled price fixture. Open an imported list and
enter offline review. In Chrome, allow only the local app origin. Reload the
offline view and check the items, quantities, checked states, retailer choice,
saved estimates, and price retrieval time against the normal view.

Attempt edit, refresh, and chat requests from the same session. Verify the
server rejects writes, the price endpoint receives no requests, and links to
other screens return to offline review. Exit and verify the same list reopens
with normal controls. The HTTP tests also check a second session, cross-origin
exit, and the message when there is no known retrieval time.

Offline review requires the local Spring Boot process. The test keeps
localhost available and blocks other origins in the browser context.

Sources: [browser suite](../../../src/test/browser/market-desk.cjs),
[HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java).
