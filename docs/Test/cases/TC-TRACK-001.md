# TC-TRACK-001 - Wishlist and explicit tracking (J05)

Covers saving a wishlist entry versus explicitly tracking it, and the lifecycle
of retained price observations.

Source: [`TrackingServiceTest`](../../../src/test/java/com/pricewatch/tracking/TrackingServiceTest.java).

Decision references: D01 (track all listed retailers), D02 (retention and
periods), D03 (observation identity and unknown prices).

Steps and expectations:

1. Save a product. Expect `isSaved` true and `isTracked` false, and no
   observations recorded — saving does not start tracking.
2. Enable tracking. Expect one enabled choice per retailer currently listed.
3. Import a changed catalogue. Expect new observations for the tracked pairs; the
   latest price matches the accepted listing.
4. Attempt an invalid import. Expect `CatalogImportException` and no new
   observations (a failed import adds nothing).
5. Import the identical bytes twice. Expect no duplicate observation (identity is
   product + retailer + checksum + period).
6. Stop tracking, import again. Expect no new collection. Re-enable. Expect a new
   `periodId` greater than the previous period.
7. Import a null price. Expect an unknown observation stored with `known=false`
   and `price=null` (provenance only; never used as a price).

Limits: tests use an in-memory H2 database and constructed imports; dates use a
controlled clock. Restart persistence is covered by the file-DB data cases.
