# TC-ALERT-003 — Independent and combined Watch rules

Use isolated H2 databases, a controlled clock, and a two-pack price fixture.
Save one exact pack to Wishlist. The other pack has a lower price.

1. Set two independent targets for the saved pack. Confirm each fires at its own threshold, groups qualifying retailers, and rearms on known prices above that target. Unknown prices and saving the same target leave the arming state intact. A deleted rule leaves its alert evidence in history.
2. Set a seven-day price and time rule. A qualifying price before the due date should not fire. Let the rule come due with a high price. Edit the target and check due again. The original due date stays pending through samples and unknown live prices, even if an older price matches the new target. The next qualifying live refresh sends one alert with the original due date, all qualifying retailers, and the actual alert time. Its next interval starts at that alert time.
3. Change the interval and confirm the due date moves. At the next due date, a qualifying accepted live catalogue can trigger the alert without another refresh. Its recorded retrieval time remains distinct from the alert time. If the accepted catalogue is a cheap sample when the rule first comes due, it must wait for a qualifying live refresh.
4. Remove the pack from Wishlist and confirm the pending rule is paused. Save it again, then force accepted-import storage to fail after alert evaluation. Neither the alert nor the new due date may commit. A successful refresh sends the alert. Read and delete the rule to confirm its history remains available.
5. Start a due check and qualifying live refresh together. Both operations complete, one occurrence is recorded, and the next date advances once.
6. Reopen a file database with a legacy target and its alert, after dropping the new table and columns to match the old schema. The old target, retailer price evidence and read state survive. Add a new rule and verify that it can create an additional alert on the same database.

Source [WatchRuleServiceTest](../../../src/test/java/com/pricewatch/notifications/WatchRuleServiceTest.java). The fixture controls source prices and the due clock. It does not use the live feed or browser.
