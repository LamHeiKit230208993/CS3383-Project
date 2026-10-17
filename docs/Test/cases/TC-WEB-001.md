# TC-WEB-001 — Root page renders Market Desk

| Field | Details |
|---|---|
| Test class | `HelloControllerTest` |
| Test method | `rootReturnsMarketDeskView()` |
| Type | Spring MVC slice test |
| Request | `GET /` |
| Setup | `@WebMvcTest(HelloController.class)` with `MockMvc` |

## Purpose

Verify the root endpoint selects the Market Desk view and renders its title.

## Steps

1. Send `GET /` through `MockMvc`.
2. Check the response status, view, content type, and browser title.

## Expected result

- HTTP status is `200 OK`.
- View name is `market-desk`.
- Content type is compatible with `text/html`.
- The page title is `Market Desk · PriceWatch`.

## Review notes

This test does not verify full application startup, browser rendering,
external services, persistence, or error handling.

Source: [`HelloControllerTest.java`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
