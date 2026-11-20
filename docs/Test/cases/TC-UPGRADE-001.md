# TC-UPGRADE-001 — Preserve the baseline database on upgrade

Source: [persistence-check.cjs](../../../src/test/browser/persistence-check.cjs).

On the current Cart, the browser waits for the checkmark save before
restarting. On older baseline builds, it uses the legacy Save changes form.

## Purpose and setup

Verify a catalogue and shopping list survive reopening an existing H2 file
with the current app. Use a saved baseline JAR, the newly packaged current JAR,
JDK 21 and Playwright/Chrome. Set `BASELINE_JAR` to the old build, then run
`npm run test:persistence`. For J01–J08 to J12, also set `UPGRADE_TRACKING=1`.

## Steps and expected result

1. Start the baseline against a new temporary file database. Import the checked
   sample, choose JASONS milk, add two packs and mark the item checked.
2. Stop the baseline and start the current JAR with the same database URL and
   normal additive schema updates.
3. Confirm the historical source status, one list item, JASONS selection, two
   packs, checked state and exact HK$25.00 line estimate remain.

In tracking mode, the legacy app first loads the controlled loopback feed
through live refresh. Save the milk to the wishlist and enable all-retailer
tracking. After reopening the file, check that the source is still saved,
all-retailer tracking is still active, and the history table includes the
recorded HK$12.50 at JASONS.

## Limits

This tests additive upgrade compatibility for the baseline tables, not every
possible historical database version or new-feature restart state. The Java
persistence tests cover new records separately. `WRITE_DELAY=0` makes the test
independent of Windows force-termination timing. The generated database is
isolated under the OS temporary directory; normal user data is never opened.
Without `BASELINE_JAR`, the script uses the current JAR twice and proves restart
only. Record whether an actual old-to-new run occurred in test history.
