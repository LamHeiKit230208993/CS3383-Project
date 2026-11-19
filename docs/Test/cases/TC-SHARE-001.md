# TC-SHARE-001 — List export and import

This case covers the J10 list-transfer file. Dated execution results are
recorded separately in the test history.

Sources:
[`ListTransferServiceTest`](../../../src/test/java/com/pricewatch/catalog/ListTransferServiceTest.java),
[`ListTransferControllerTest`](../../../src/test/java/com/pricewatch/web/ListTransferControllerTest.java), and
the list-transfer scenario in `src/test/browser/milestone-check.cjs`.

## Export

Add a product with a saved retailer, quantity, and checked state. Request
`GET /list/export`. Expect an attachment with a `.pricewatch.json` name,
`application/json`, and a version 1 object containing `format`, `version`,
`exportedAt`, `name`, and one item per saved product with `productCode`,
`quantity`, `checked`, and `retailerCode`. Prices are not stored in the file.
`GET /list/export?listId=...` exports an imported list with its own name.
An unknown list id returns 404.

## Import

Upload a version 1 file from `/list/import`. Expect one new list, a preview with
its own id, and unchanged records: product, quantity, checked state, and saved
retailer code. A name collision appends `(imported)` and then a number. A blank
name becomes the default name. The existing My list is never overwritten or
merged; its items stay unchanged. An imported list can be exported again.

A retailer that is listed for the product but currently has no price keeps its
saved choice; its price remains unknown rather than switching to another
retailer, matching the pinned-retailer rule in TC-LIST-001.

## Rejection

The whole file is validated before any write. Expect rejection and no new list
or item for: an empty file, a file over 2 MB, malformed JSON, a non-object root,
the wrong `format`, a missing or non-1 `version`, missing or empty `items`, more
than 999 items, a non-object item, a missing or blank product code, a repeated
product code, a missing or non-integral quantity, a quantity outside 1–999, a
missing or non-boolean `checked` value, an unknown or inactive product, and a
retailer code that is not listed for that product. A rejected upload leaves an
existing list and its items unchanged.

The browser scenario also imports a malformed file after a successful import
and checks that the earlier imported list remains. Limits: 2 MB, 999 distinct
products, local single-user storage. The file is a snapshot, not a live link.
