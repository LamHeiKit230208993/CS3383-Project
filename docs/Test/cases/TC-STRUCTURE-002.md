# TC-STRUCTURE-002 — Shared model attributes and view formatting

Sources: [`GlobalModelAttributes`](../../../src/main/java/com/pricewatch/web/GlobalModelAttributes.java),
[`ViewFormat`](../../../src/main/java/com/pricewatch/web/ViewFormat.java),
[`HelloControllerModelTest`](../../../src/test/java/com/pricewatch/web/HelloControllerModelTest.java),
[`HelloControllerTest`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java),
and [`ViewFormatTest`](../../../src/test/java/com/pricewatch/web/ViewFormatTest.java).

Every screen receives `importStatus` and `format` from one controller advice,
not from each controller. Templates keep using
`${format.money(...)}`, `${format.time(...)}`, and
`${format.hasListingPrice(...)}`.

Automated checks:

- `HelloControllerModelTest` registers the advice and `ViewFormat`; its existing
  model assertions still pass.
- `HelloControllerTest` renders every J01–J04 screen with unchanged output.
- `ViewFormatTest` checks an unknown price (`Not listed`), four-decimal money
  formatting, and Hong Kong time formatting.

```powershell
.\mvnw.cmd '-Dtest=HelloControllerTest,HelloControllerModelTest,ViewFormatTest' test
```

This checks the shared template contract on the existing screens. New J05–J12
screens must reuse the same attributes rather than adding their own copies.
