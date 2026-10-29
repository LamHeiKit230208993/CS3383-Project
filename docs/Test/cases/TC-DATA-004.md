# TC-DATA-004 — Flyway baseline and schema validation

Sources: [`MigrationTest`](../../../src/test/java/com/pricewatch/catalog/MigrationTest.java),
[`MigrationLegacyBaselineTest`](../../../src/test/java/com/pricewatch/catalog/MigrationLegacyBaselineTest.java),
and [`CatalogPersistenceTest`](../../../src/test/java/com/pricewatch/catalog/CatalogPersistenceTest.java).

The application must create its schema through Flyway and leave Hibernate in
`validate` mode.

Automated checks:

- `MigrationTest` starts against an empty in-memory database and confirms a
  successful `flyway_schema_history` entry for version 1. It also checks the six
  baseline tables and the two unique constraints.
- `MigrationLegacyBaselineTest` first creates a database with Hibernate `create`
  and inserts a legacy product. It then reopens the same file with Flyway
  enabled, `baseline-on-migrate=true`, and `ddl-auto=validate`. The schema is
  marked as a version 1 baseline and the legacy row survives.
- `CatalogPersistenceTest` now runs with `ddl-auto=validate`, covering
  create-on-first-run, catalogue import, list edits, and reopen.

```powershell
.\mvnw.cmd '-Dtest=MigrationTest,MigrationLegacyBaselineTest,CatalogPersistenceTest' test
```

This proves the baseline and legacy-adoption paths on H2. It does not prove a
future `V2` migration, a PostgreSQL schema, or concurrent access.
