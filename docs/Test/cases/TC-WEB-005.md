# TC-WEB-005 — Compare route remains visible after JavaScript loads

| Field | Details |
|---|---|
| Type | Browser regression check |
| Request | `GET /compare/{code}` on the running application |
| Setup | Start the Spring Boot application with an imported catalogue and open a product comparison link |

## Purpose

Verify that the shared navigation script does not hide the server-rendered
comparison page. Product names and the compare-and-add buttons both link to
this route.

## Steps

1. Open the Discover page.
2. Select a product name or its compare-and-add button.
3. Wait for `market-desk.js` to finish loading.
4. Confirm that the comparison heading, product details, and retailer price
   table are visible.

## Expected result

The `scene-compare` section remains visible after the script runs, and the
comparison page does not become blank. Any `/compare/{code}` path is treated
as the `compare` scene.

## Limits

This is a browser behavior check rather than an automated Maven test. It does
not validate unknown product redirects or add-to-list POST behavior; those are
covered by [`TC-WEB-002`](./TC-WEB-002.md) and
[`TC-LIST-001`](./TC-LIST-001.md).

Source: [`market-desk.js`](../../../src/main/resources/static/js/market-desk.js)
