# TC-ALERT-005 Configurable price drops

Use isolated H2 and a fixed clock. Save the exact milk pack to Wishlist and
create price-drop rules for HK$3 and 25%. Accept a live snapshot of HK$12
and HK$8 at two retailers, followed by HK$9 and HK$6.

- The threshold includes equality. The percentage rule lists both retailers.
  The amount rule lists only the first. History retains prior and final amounts.
- A missing baseline, a sample import, and a zero percentage denominator
  produce no drop event. Reject zero thresholds and percentages above 100.
- Create a percentage rule through Alerts and change it to an amount rule.
  Verify the editor shows the selected kind and defaults new price rules to Daily.
- Prepare a percentage rule through Ask. Verify the confirmation card appears
  before any write and the saved rule uses Daily unless the shopper picks
  a different schedule.
- For a due calendar-and-price rule, a qualifying cached listing leaves the
  rule pending. A later accepted qualifying live snapshot fires it.
- In Chrome, open the editor with the keyboard, create both drop kinds,
  refresh the fixture feed, and verify prior and current retailer amounts.
  Check History at desktop and phone widths in all three languages.

Sources are [WatchRuleServiceTest](../../../src/test/java/com/pricewatch/notifications/WatchRuleServiceTest.java),
[AlertCenterControllerTest](../../../src/test/java/com/pricewatch/web/AlertCenterControllerTest.java),
and [AskShoppingToolsTest](../../../src/test/java/com/pricewatch/web/AskShoppingToolsTest.java).
