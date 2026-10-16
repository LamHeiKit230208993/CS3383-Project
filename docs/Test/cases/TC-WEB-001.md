# TC-WEB-001 — Root page renders Hello view

| Field | Details |
|---|---|
| Test class | `HelloControllerTest` |
| Test method | `rootReturnsHelloViewWithMessage()` |
| Type | Spring MVC slice test |
| Request | `GET /` |
| Setup | `@WebMvcTest(HelloController.class)` with `MockMvc` |

## Purpose

Verify the root endpoint selects the expected view and supplies its greeting.

## Steps

1. Send `GET /` through `MockMvc`.
2. Check the response status, view, and model.

## Expected result

- HTTP status is `200 OK`.
- View name is `hello`.
- Model attribute `message` is `Hello, World!`.

## Review notes

This test does not verify full application startup, browser rendering,
external services, persistence, or error handling.

Source: [`HelloControllerTest.java`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
