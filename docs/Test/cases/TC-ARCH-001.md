# TC-ARCH-001 — Import event after commit only

Source: [`CatalogueEventTest`](../../../src/test/java/com/pricewatch/catalog/CatalogueEventTest.java).

`CatalogReplacementService` publishes `CatalogueReplacedEvent` inside the
replacement transaction. A recording listener uses
`@TransactionalEventListener(phase = AFTER_COMMIT)`.

Automated checks:

- A successful sample import produces exactly one event, carrying the accepted
  checksum and product count.
- A forced database failure (a temporary H2 CHECK constraint rejects the new
  retailer code) rolls back the import and produces no event.
- The test is not wrapped in `@Transactional`, so the commit and after-commit
  callback both really happen.

```powershell
.\mvnw.cmd '-Dtest=CatalogueEventTest' test
```

This checks the event contract, not listener performance or scheduler timing.
The logged constraint violation is deliberate.
