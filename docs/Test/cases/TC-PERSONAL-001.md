# TC-PERSONAL-001 · Personal data export and reset

Use a dedicated in-memory H2 database with the checked sample. Seed a default
list and an imported copy, a wishlist entry, tracking with an observation
and alert, a reminder, a budget, display and notification preferences, and two
separate browser sessions with Ask chats. The imported copy references a product
that is absent from the catalogue.

1. Open Personal data and export its JSON. Check all record categories, the
   saved quantity, checked state, retailer choice, and missing-product
   display name. Check that the export contains the requesting session's
   chat and that a catalogue is absent from the file.
2. Open the reset confirmation, then return to Personal data. Check that the
   overview has the same list and item counts. Submit the reset without
   confirmation and check the confirmation error and retained settings.
3. Use a temporary foreign key to block deletion of the imported-only product
   at the end of the reset. Submit a confirmed reset and check that the
   failure preserves the lists, preferences, tracking flag, and session chat.
4. Remove the temporary constraint and confirm reset. Check that personal
   records and the imported-only product are cleared. Check that the source
   catalogue, lowest price, and import metadata are still available. Check
   that the requesting session is invalidated and the other session remains
   active.

The test makes no external price or chat calls. It tests HTTP routes and H2
transactions. The Chrome suite also follows the actual confirmation and
download controls.

Source [PersonalDataControllerTest](../../../src/test/java/com/pricewatch/web/PersonalDataControllerTest.java).
