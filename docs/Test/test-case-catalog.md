# Test Case Catalog

Use this brief index to review the automated test suite. Keep one detailed
document per test case in [`cases/`](./cases/).

| ID | Area | Test | Expected result | Source |
|---|---|---|---|---|
| [TC-WEB-001](./cases/TC-WEB-001.md) | Web MVC | `GET /hello` renders the Hello fallback | `200 OK`, view `hello`, message `Hello, World!` | [`HelloControllerTest.java`](../../src/test/java/com/pricewatch/web/HelloControllerTest.java) |
| [TC-WEB-002](./cases/TC-WEB-002.md) | UI prototype | Prototype routes and sample content | `/` redirects to `/search`; every screen returns `200 OK` with its view and sample labels | [`PrototypeControllerTest.java`](../../src/test/java/com/pricewatch/web/PrototypeControllerTest.java) |
| [TC-WEB-003](./cases/TC-WEB-003.md) | UI prototype | No external asset requests | Templates and static files contain no `http://` or `https://` asset URLs | [`PrototypeControllerTest.java`](../../src/test/java/com/pricewatch/web/PrototypeControllerTest.java) |

## Review status

- Current automated cases: **13 test methods across 2 classes**
- Current coverage: Hello fallback, prototype routes, key screen markup, and
  external-asset scan
- Known gaps: JavaScript interaction, browser layout, visual contrast, real
  product data, storage, import, AI, and notification services

## Prototype check record — 20 October 2026

- `.\mvnw.cmd test` passed: `HelloControllerTest` (1 test) and
  `PrototypeControllerTest` (12 tests), with no failures or errors.
- The prototype is sample-data only. Passing tests do not prove real data
  import, saved storage, AI calls, or notifications.
