# TC-TRACK-001 — Wishlist, retailer tracking, and retained prices

## Purpose

Check that saving a product is separate from tracking it, that tracking uses one
exact product and retailer, and that price history survives catalogue
replacement and a stopped tracking period.

## Setup

- Start the Spring test application with an isolated H2 database.
- Use a small constructed catalogue with one product and two retailer listings.
- Mark fixtures as `SAMPLE` or `LIVE` in the import call. Do not use the
  historical sample to claim live observations.

## Steps

1. Accept a `SAMPLE` catalogue, save the product, and start tracking one exact
   retailer. Confirm the wishlist entry exists and the tracking period has no
   observations from the sample.
2. Accept a `LIVE` catalogue with a known price for the selected retailer.
   Confirm one observation records the product, retailer, retrieval time, source
   URL, and source checksum.
3. Accept the same price again. Confirm no duplicate observation is added.
4. Accept a missing or unknown price, then repeat that unknown state. Confirm a
   single null gap is kept and the repeated gap is skipped.
5. Accept a known price after the gap. Confirm it is recorded, while drop
   evaluation does not compare across the unknown gap.
6. Stop tracking and accept another `LIVE` catalogue. Confirm the stopped period
   gains no new observation and its earlier history remains.
7. Start tracking the same product and retailer again. Confirm a new period is
   created and does not compare against the stopped period's observations.
8. Delete history for a period. Confirm its observations and associated price
   alert events are removed, while the wishlist entry and tracking period remain.
9. Force the accepted-import write to fail after observation/event creation has
   started. Confirm the catalogue, accepted-import metadata, observations, and
   events all roll back.
10. Add an observation older than 90 days, open the tracking view, and confirm
    the expired observation is removed.
11. After an observation expires, refresh the same current LIVE price. Confirm
    the expired row stays removed. Set a qualifying target and confirm it uses
    the accepted listing without restoring that history row.
12. After history expires, import a changed price. Confirm it starts a new
    comparison baseline and cannot trigger a drop against expired history.
13. Send an old LIVE import with a changed price. Confirm it is not recorded as
    a current observation or target event.
14. Close and reopen the file-backed app. Confirm wishlist, tracking, history,
    target/drop events, read/snooze state and settings remain. Re-import the same
    LIVE price and confirm no additional observation or alert appears.

## Expected result

Start product-wide tracking from Saved with one retailer. Starting again
keeps one active period. A SAMPLE import with a second retailer adds no
period. The next LIVE import enrolls that retailer with its own baseline
and no cross-retailer drop. Repeating the import adds nothing. Stop all
retailers and import a third listing. Existing history remains available
and the third retailer is excluded. The file-backed restart test also checks
that the product-wide preference survives reopening the database.

Only successfully accepted `LIVE` imports add observations to enabled periods.
The exact product/retailer identity is kept. Saving alone never starts tracking.
Unknown prices break drop comparisons. Stopping preserves past observations;
restarting creates a separate period. Retained price state suppresses repeated
unchanged imports after observation rows expire; a changed price starts a new
comparison baseline. A current LIVE listing may qualify for a target without
restoring an expired history row, but stale LIVE data cannot. Failed replacement
rolls back newly written history and alert events.

## Source

- [TrackingServiceTest](../../../src/test/java/com/pricewatch/catalog/TrackingServiceTest.java)
- [AlertServiceTest](../../../src/test/java/com/pricewatch/notifications/AlertServiceTest.java)
- [TrackingPersistenceRestartTest](../../../src/test/java/com/pricewatch/catalog/TrackingPersistenceRestartTest.java)
- [TrackingService](../../../src/main/java/com/pricewatch/tracking/TrackingService.java)
- [CatalogReplacementService](../../../src/main/java/com/pricewatch/catalog/importer/CatalogReplacementService.java)

## Limits and review notes

Tests use constructed imports and a local database. They do not contact the
Consumer Council feed or prove current prices. There is no cloud collection or
notification delivery while the app is closed. History is retained for 90 days
in the local app.
