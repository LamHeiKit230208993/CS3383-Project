# TC-UPGRADE-001 — Preserve the baseline database on upgrade

Source: [persistence-check.cjs](../../../src/test/browser/persistence-check.cjs).

## Purpose and setup

Verify an existing J01–J04 catalogue and shopping list survive reopening the
same H2 file with the J05–J08 application. Use a saved baseline JAR, the newly
packaged current JAR, JDK 21 and Playwright/Chrome. Set `BASELINE_JAR` to the
old build, then run `npm run test:persistence`.

## Steps and expected result

1. Start the baseline against a new temporary file database. Import the checked
   sample, choose JASONS milk, add two packs and mark the item checked.
2. Stop the baseline and start the current JAR with the same database URL and
   normal additive schema updates.
3. Confirm the historical source status, one list item, JASONS selection, two
   packs, checked state and exact HK$25.00 line estimate remain.

## Limits

This tests additive upgrade compatibility for the baseline tables, not every
possible historical database version or new-feature restart state. The Java
persistence tests cover new records separately. `WRITE_DELAY=0` makes the test
independent of Windows force-termination timing. The generated database is
isolated under the OS temporary directory; normal user data is never opened.
Without `BASELINE_JAR`, the script uses the current JAR twice and proves restart
only. Record whether an actual old-to-new run occurred in test history.
