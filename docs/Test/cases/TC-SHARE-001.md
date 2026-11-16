# TC-SHARE-001 — Separate-copy list sharing (J10)

Covers export and import of a shopping list as a separate copy (R32). A
downloaded JSON file is reviewed in a preview, then merged into My list. The
format was decided on 15 November 2026. Dated execution results are recorded
separately in the test history.

Sources: [`ListTransferServiceTest`](../../../src/test/java/com/pricewatch/shopping/ListTransferServiceTest.java),
[`HelloControllerTest`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java).

## Export

`GET /list/export` returns an `application/json` attachment named
`market-desk-list.json`. The body has `format` (`market-desk-list/1`), `listName`,
`exportedAt`, and `items` with `code`, `quantity`, `checked`, and `retailerCode`.
Only the chosen My list is exported; no accounts, wishlist or tracking data.

## Round trip

Save a product with quantity, checked state and a chosen retailer. Export, clear
the database, re-import the file, and expect the same products, quantities,
checked state and saved retailer. An automatic (lowest-price) item keeps a null
retailer.

## Preview then merge

Uploading the file without `confirm=true` renders a preview that lists each
product (name, quantity, checked state, retailer) plus the export time and item
count, and changes no list rows. Submitting `confirm=true` merges the items into
My list and shows a notice. The uploaded file is held in the HTTP session between
the two steps.

## Rejection (no partial write)

Malformed JSON, a wrong `format`, an out-of-range quantity, a product absent from
the accepted catalogue, or a conflicting retailer on an item already in My list
rejects the **whole** import with a message and leaves My list unchanged.

## Keep unpriced

An imported item whose saved retailer has no current price keeps that retailer
and shows an unknown amount. Unlike the normal add form, an explicit retailer in
an import does not need a current price.

A merged item for a product already in My list combines its quantity and adopts
the imported checked state; a different saved retailer is rejected rather than
overwritten. Later edits do not sync between the exported copy and the original.
