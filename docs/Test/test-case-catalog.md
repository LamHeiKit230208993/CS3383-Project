# Test Case Catalog

Use this brief index to review the automated test suite. Keep one detailed
document per test case in [`cases/`](./cases/).

| ID | Area | Test | Expected result | Source |
|---|---|---|---|---|
| [TC-WEB-001](./cases/TC-WEB-001.md) | Web MVC | `GET /` renders the Hello page | `200 OK`, view `hello`, message `Hello, World!` | [`HelloControllerTest.java`](../../src/test/java/com/pricewatch/web/HelloControllerTest.java) |

## Review status

- Current automated cases: **1**
- Current coverage: root-page MVC behavior
- Known gaps: product data, search, price comparison, API failures, and
  end-to-end flows
