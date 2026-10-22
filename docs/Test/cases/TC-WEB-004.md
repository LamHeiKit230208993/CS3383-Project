# TC-WEB-004 — List route remains visible after JavaScript loads

| Field | Details |
|---|---|
| Type | Browser regression check |
| Request | `GET /list` on the running application |
| Setup | Start the Spring Boot application with the sample catalogue and open `/list` |

## Purpose

Verify that the shared navigation script does not hide the server-rendered
list page. The list template uses the `scene-list` section, while the
JavaScript navigation logic controls section visibility.

## Steps

1. Open `http://localhost:8080/list`.
2. Wait for `market-desk.js` to finish loading.
3. Confirm that the list heading, item rows or empty state, and list summary
   are visible.
4. Confirm that the "My list" navigation entry is selected.

## Expected result

The `scene-list` section remains visible after the script runs, and the page
does not become blank. The list route is treated as the `list` scene based on
the request path.

## Limits

This is a browser behavior check rather than an automated Maven test. It does
not validate list persistence or POST actions; those are covered by
[`TC-LIST-001`](./TC-LIST-001.md) and the server-rendered portion of
[`TC-WEB-003`](./TC-WEB-003.md).

Source: [`market-desk.js`](../../../src/main/resources/static/js/market-desk.js)
