# TC-WEB-001 — Discover page lists the imported catalogue

| Field | Details |
|---|---|
| Test class | `PageControllerTest` |
| Test methods | `homeListsImportedProducts()`, `searchFindsByTraditionalChineseName()`, `searchFindsBySimplifiedChineseName()` |
| Type | Full context test rendering the real Thymeleaf template |
| Request | `GET /`, `GET /?q=鮮牛奶`, `GET /?q=鲜牛奶` |
| Setup | `@SpringBootTest` with `MockMvc`; the sample is imported in `@BeforeEach` |

## Purpose

Verify that the real page, not a mock, shows the stored products, the
retrieval time, and the unknown source date, and that Chinese search reaches
the page.

## Steps

1. `GET /` and read the rendered HTML.
2. Search with a Traditional Chinese name.
3. Search with the Simplified Chinese name.

## Expected result

- The page contains the bread product name and a retrieval time.
- The page states that the source date is unknown.
- `鮮牛奶` returns `P000000124` and does not return the bread.
- `鲜牛奶` returns both milk codes.

## Limits

Checks rendered HTML strings, not layout or JavaScript. It does not exercise
the live download.

Source: [`PageControllerTest.java`](../../../src/test/java/com/pricewatch/web/PageControllerTest.java)
