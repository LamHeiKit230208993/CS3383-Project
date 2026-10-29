# TC-TIME-001 — Injected clock controls retrieval time

Source: [`CatalogClockTest`](../../../src/test/java/com/pricewatch/catalog/CatalogClockTest.java).

`CatalogService` takes a `Clock` and uses `clock.instant()` when accepting an
import. The test imports [`FixedClockConfiguration`](../../../src/test/java/com/pricewatch/support/FixedClockConfiguration.java),
which supplies a fixed instant in place of the system clock.

Automated checks:

- A sample import stores the fixed instant as its retrieval time.
- `catalog.status()` reports the same fixed instant.

```powershell
.\mvnw.cmd '-Dtest=CatalogClockTest' test
```

This proves deterministic time injection. It does not test scheduling, quiet
hours, snooze, or time-zone boundaries; those belong to J06 and J08.
