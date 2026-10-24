# TC-WEB-002 — Root page includes prototype scenes and assets

> Retired prototype case. See TC-WEB-001 for current baseline page checks.

| Field | Details |
|---|---|
| Test class | `HelloControllerTest` |
| Test method | `rootIncludesPrototypeScenesAndLocalAssets()` |
| Type | Spring MVC slice test |
| Request | `GET /` |

## Purpose and steps

Send `GET /`. Check the sample labels, scene sections, stylesheet reference,
and deferred script reference in the rendered HTML.

## Expected result

- The response includes `UI prototype` and `Sample data`.
- A section exists for each scene: Discover, Compare, List, Saved, Updates,
  and Assistant.
- A stylesheet link points to `/css/market-desk.css`.
- A deferred script points to `/js/market-desk.js`.

## Limits

The test checks asset references, not whether a browser can load or run them.
It does not check scene switching, charts, or visual layout.

Source: [`HelloControllerTest.java`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
