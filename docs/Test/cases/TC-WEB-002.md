# TC-WEB-002 — UI prototype routes and sample content

| Field | Details |
|---|---|
| Test class | `PrototypeControllerTest` |
| Test methods | `rootRedirectsToSearch`, `everyPrototypeScreenResolvesToItsTemplate`, `comparePageCanShowAnotherSampleProduct`, and screen content checks |
| Type | Spring MVC slice test |
| Requests | `GET /`, `/search`, `/compare`, `/list`, `/saved`, `/alerts`, `/history`, `/assistant`, `/settings` |
| Setup | `@WebMvcTest(PrototypeController.class)` with `MockMvc` |

## Purpose

Verify that the UI prototype routes resolve to their templates and that each
screen contains the expected sample-data controls and labels.

## Steps

1. Send `GET /` and check the redirect.
2. Send each screen request and check status, view name, and the sample banner.
3. Check key markup:
   - search input label and comparison link
   - product code, tied and missing retailer states
   - list quantity attributes and estimated total
   - separate wishlist and tracking sections
   - target price, quiet hours, and snooze controls
   - available-date and empty-history states
   - checked sample comparison and no-AI note
   - language and high-contrast controls

## Expected result

- Root redirects to `/search`.
- Every screen returns `200 OK` with the expected view.
- Every screen contains `Sample data`.
- The listed controls and labels are present.

## Limits

The tests check server-rendered HTML. They do not run JavaScript, compute CSS,
prove browser layout, or verify real data, storage, AI, or notifications.

Source: [`PrototypeControllerTest.java`](../../../src/test/java/com/pricewatch/web/PrototypeControllerTest.java)
