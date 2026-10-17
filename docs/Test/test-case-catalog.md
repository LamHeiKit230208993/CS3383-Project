# Test Case Catalog

Use this brief index to review the automated test suite. Keep one detailed
document per test case in [`cases/`](./cases/).

| ID | Area | Test | Expected result | Source |
|---|---|---|---|---|
| [TC-WEB-001](./cases/TC-WEB-001.md) | Web MVC | `GET /` renders Market Desk | `200 OK`, HTML, view `market-desk`, expected page title | [`HelloControllerTest.java`](../../src/test/java/com/pricewatch/web/HelloControllerTest.java) |
| [TC-WEB-002](./cases/TC-WEB-002.md) | Web MVC | Root page includes prototype scenes and assets | Sample labels, six scenes, local CSS and deferred JavaScript | [`HelloControllerTest.java`](../../src/test/java/com/pricewatch/web/HelloControllerTest.java) |
| [TC-WEB-003](./cases/TC-WEB-003.md) | Web MVC | Root page includes search and demo controls | Visible search label tied to its input, main landmark, navigation, and dialogs | [`HelloControllerTest.java`](../../src/test/java/com/pricewatch/web/HelloControllerTest.java) |

## Review status

- Current automated cases: **3**
- Current coverage: root-page MVC view and key HTML structure
- These cases do not run JavaScript or prove browser interaction, visual
  layout, keyboard behaviour, real price calculations, or saved data.
- Test execution results must come from the current Maven output. The table
  defines the checks; dated records below state the observed run results.

## Prototype check record — 17 October 2026

- The implementation agent ran the focused `HelloControllerTest` class after
  the screen shell and again after adding demo interactions. Both runs passed
  all three cases.
- Full suite: `.\mvnw.cmd -q test` passed all three tests, with no failures,
  errors, or skipped tests.
- Chrome browser checks: **62 of 62 passed**. All six scenes fit widths of
  375, 768, 1024, and 1440 pixels without horizontal overflow.
- Browser checks covered saved-item and tracking focus, quantity validation,
  dialogs closed with Escape, high contrast, Chinese-name search, tied prices,
  missing listings, reduced motion, and demo reset after reload.
- The browser run reported no console errors, page errors, or external
  requests.
- Limits: the browser checks used Chrome only. Scripted checks are not a full
  accessibility audit. These results do not prove real data import, saved
  storage, AI calls, or notifications; those are outside this UI prototype.
