# TC-WEB-003 — Root page includes search and demo controls

> Retired prototype case. See TC-WEB-001 for current baseline page checks.

| Field | Details |
|---|---|
| Test class | `HelloControllerTest` |
| Test method | `rootProvidesNamedSearchAndReachableDemoControls()` |
| Type | Spring MVC slice test |
| Request | `GET /` |

## Purpose and steps

Send `GET /`. Check the actual HTML elements for the search label and input,
main landmark, Discover navigation link, and native dialogs.

## Expected result

- A non-empty visible label targets input `product-search`.
- The main landmark has ID `main-content`.
- A navigation element contains a link to `#discover`.
- Native dialogs exist with IDs `settings-dialog` and `quantity-dialog`.

## Limits

This test checks page structure. Browser checks must cover focus, dialog
opening and closing, search results, and quantity changes.

Source: [`HelloControllerTest.java`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
