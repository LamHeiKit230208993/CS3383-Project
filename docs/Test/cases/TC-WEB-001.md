# TC-WEB-001 — Hello fallback page renders

| Field | Details |
|---|---|
| Test class | `HelloControllerTest` |
| Test method | `helloReturnsHelloViewWithMessage()` |
| Type | Spring MVC slice test |
| Request | `GET /hello` |
| Setup | `@WebMvcTest(HelloController.class)` with `MockMvc` |

## Purpose

Verify the retained fallback endpoint selects the Hello view and supplies its
greeting. The root route belongs to the UI prototype and redirects to `/search`.

## Steps

1. Send `GET /hello` through `MockMvc`.
2. Check the response status, view, and model.

## Expected result

- HTTP status is `200 OK`.
- View name is `hello`.
- Model attribute `message` is `Hello, World!`.

## Review notes

This test does not verify full application startup, browser rendering,
external services, persistence, or error handling.

Source: [`HelloControllerTest.java`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
