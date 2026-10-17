# TC-WEB-002 — Search page comparison card link

| Field | Details |
|---|---|
| Test class | `PageControllerTest` |
| Test method | `searchIncludesTheComparisonCardLink()` |
| Type | Spring MVC slice test |
| Request | `GET /search` |
| Setup | `@WebMvcTest(PageController.class)` with `MockMvc` |

## Purpose

Verify that the search page keeps the comparison card as a link to the
history page. The card's block presentation and link reset are supplied by
the `.compare` CSS rule in `app.css`.

## Steps

1. Send `GET /search` through `MockMvc`.
2. Check the response status.
3. Check that the response contains the `.compare` anchor and its
   Thymeleaf history link.
4. In a browser, confirm the rendered anchor is a block-level card without
   the default underline.

## Expected result

- HTTP status is `200 OK`.
- The response contains `<a class="compare"`.
- The comparison card points to `/history`.
- Browser rendering shows the full-width card styling from `.compare`.

## Limitations

The automated assertions do not compute browser CSS or layout dimensions.
The final display and text-decoration checks require browser inspection.

Source: [`PageControllerTest.java`](../../../src/test/java/com/pricewatch/web/PageControllerTest.java)
