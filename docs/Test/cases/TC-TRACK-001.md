# TC-TRACK-001 — Wishlist and explicit tracking

This case covers the J05 saved/tracked workflow and price-observation recording.
Dated execution results are recorded separately in the test history.

Sources:
[`TrackingServiceTest`](../../../src/test/java/com/pricewatch/catalog/TrackingServiceTest.java),
[`HelloControllerTest`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java).

Saving a product (`POST /saved/{code}/save`) must save it for later. Saving alone
must **not** start tracking: `isTracked` stays false and no observations exist.
Enabling tracking (`POST /tracking/{code}/enable`) records a choice for each
priced retailer listing of the product (D01); a listing without a price is
skipped. The Saved page shows the tracked state and retailer list.

After tracking is enabled, each **successful** import records one observation per
tracked (product, retailer) pair with a known price (D03). An unknown price adds
no row. Re-importing the identical file (same checksum) does not duplicate rows.

A **failed** import (invalid JSON) must add no observations and keep the accepted
catalogue. Disabling tracking (`POST /tracking/{code}/disable`) stops new
collection but keeps existing history (D02). Observations survive catalogue
replacement because they are stored in separate tables, not linked to the deleted
listing rows; an offer snapshot is kept with each observation for provenance.
Removing a saved product does not delete tracking or history.

Expectations: state and history persist across restart; the source retrieval time
is labelled and the source price date stays unknown.
