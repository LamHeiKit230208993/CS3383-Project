# Test Case Catalog

Use this brief index to review the automated test suite. Keep one detailed
document per test case in [`cases/`](./cases/).

| ID | Area | Test | Expected result | Source |
|---|---|---|---|---|
| [TC-WEB-001](./cases/TC-WEB-001.md) | Web MVC | `GET /` redirects to the list page | `3xx`, redirect `/list` | [`PageControllerTest.java`](../../src/test/java/com/pricewatch/web/PageControllerTest.java) |
| [TC-WEB-002](./cases/TC-WEB-002.md) | Search UI | `GET /search` includes the comparison card link | `200 OK`, comparison card links to `/history` | [`PageControllerTest.java`](../../src/test/java/com/pricewatch/web/PageControllerTest.java) |

## Review status

- Current automated cases: **2**
- Current coverage: root redirect and search-page comparison-card markup
- Known gaps: product data, search, price comparison, API failures, and
  end-to-end flows
