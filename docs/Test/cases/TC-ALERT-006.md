# TC-ALERT-006 In-app alert pop-ups

Use the isolated H2 alert-controller test and the Chrome milestone suite.
The controller test creates a qualifying live price and a due time rule,
then checks that the toast feed shows both and later omits read and expired
events while History retains them.

In Chrome, begin a search on Discover and simulate a new price event. Verify
that the bottom-right toast appears without changing the search. Dismiss it
and reload in the same tab. Verify that the same event stays hidden. Switch to
a phone viewport, simulate a reminder, and verify that the toast clears the
navigation dock and its View alert link opens History.

Chrome uses controlled responses and a fake browser clock. The test does not
require the live price feed or delivery while the app is closed.

Sources [AlertCenterControllerTest](../../../src/test/java/com/pricewatch/web/AlertCenterControllerTest.java)
and [milestone browser suite](../../../src/test/browser/milestone-check.cjs).
