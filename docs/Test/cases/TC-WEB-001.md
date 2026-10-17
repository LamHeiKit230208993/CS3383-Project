# TC-WEB-001 — Root page redirects to list

| Field | Details |
|---|---|
| Test class | `PageControllerTest` |
| Test method | `rootRedirectsToList()` |
| Type | Spring MVC slice test |
| Request | `GET /` |
| Setup | `@WebMvcTest(PageController.class)` with `MockMvc` |

## Purpose

Verify the root endpoint sends users to the list page.

## Steps

1. Send `GET /` through `MockMvc`.
2. Check the response status and redirect location.

## Expected result

- The response is a `3xx` redirect.
- The redirect location is `/list`.

## Review notes

This test does not verify full application startup, browser rendering,
external services, persistence, or error handling.

Source: [`PageControllerTest.java`](../../../src/test/java/com/pricewatch/web/PageControllerTest.java)
