# TC-ALERT-002 — Price targets across retailers

Use the test's isolated H2 database and controlled clock. Import a catalogue
with two exact packs, save one pack, and set its target to HK$10.

1. Import a historical sample and then a live snapshot with all prices
   missing. Neither creates an alert.
2. Accept a live snapshot with two retailers at or below HK$10 for the saved
   pack. Expect one alert containing both retailer prices and the retrieval
   time. The other pack has its own identity and never qualifies for this rule.
3. Save the same target again and import the same prices, followed by an
   unknown-only snapshot and another qualifying price. Count one alert.
   Then import known prices above the target and cross the target again.
   Count two alerts. The earlier alert retains its recorded prices.
4. Remove the pack from Wishlist and import qualifying prices. Verify the
   rule is retained and no event fires until the pack is saved again.
   Check read and snooze state, then delete the rule. The alert survives
   deletion. Personal-data reset can remove the alert and its retailer
   evidence together.
5. Force the accepted-import write to fail after evaluation. The catalogue
   and alert history stay as they were, and the next qualifying successful
   refresh can still trigger the rule.

Source [ProductTargetServiceTest](../../../src/test/java/com/pricewatch/notifications/ProductTargetServiceTest.java).
