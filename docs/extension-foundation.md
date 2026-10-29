# Extensibility foundation for J05–J12

This note records the shared foundations added before J05–J12. It adds no
user-facing feature: the J01–J04 routes, screens, import rules, list rules, and
totals are unchanged.

## Package boundaries

Existing packages:

| Package | Responsibility |
|---|---|
| `catalog` | Source parsing, catalogue replacement, search, and comparison views |
| `shopping` | The single saved list, quantities, checked state, and totals |
| `web` | Controllers, request guards, shared model attributes, view formatting |

Packages reserved for the next jobs:

| Package | Job |
|---|---|
| `history` | J05 wishlist, tracked products, and price observations |
| `alerts` | J06 alert rules, quiet hours, snooze, and summaries |
| `budget` | J07 budgets and category totals |
| `reminders` | J08 interval/use-rate reminders and snooze |
| `assistant` | J09 checked price facts and the language-model port |
| `transfer` | J10 versioned list transfer and preview |
| `offline` | J11 read-only saved-list snapshot |
| `preferences`, `privacy` | J12 languages, display settings, and personal-data export/delete |

## Dependency rule

`web → feature services → repositories/entities`

- A feature service may read the public views of `catalog` and `shopping`.
- A feature must not reach into another feature's repositories or entities.
- Cross-feature reactions use the import event below, not direct calls back into
  the importer.
- Controllers do not fetch source JSON, calculate money, or own shared template
  attributes. Use `ViewFormat` and `GlobalModelAttributes` instead.

## Import event contract

`CatalogReplacementService` publishes
`CatalogueReplacedEvent(ImportStatus status)` inside its catalogue-replacement
transaction, after the accepted-import row is saved.

- Listeners must use
  `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
- A failed or rolled-back import must not run listeners and must not create
  history, alerts, reminders, or other derived records.
- Listeners receive the accepted `ImportStatus`, including checksum and counts,
  so they do not need to re-read the import metadata.

## Migration policy

- Flyway owns the schema. `src/main/resources/db/migration/V1__baseline.sql` is
  the J01–J04 baseline.
- Hibernate runs with `ddl-auto=validate`; it never creates or alters tables.
- Add `V2__…`, `V3__…`, and so on for later jobs. Never edit an applied
  migration.
- Existing databases are baselined at version 1 with
  `baseline-on-migrate=true`, so no existing data is recreated.
- Back up `data/pricewatch.mv.db` before the first run of a schema-changing
  version. See the [setup guide](SETUP.md#local-database).

## Time rule

Inject `Clock` and `ZoneId`; do not call `Instant.now()` or hard-code a zone.

- `TimeConfiguration` supplies the `pricewatch.zone-id` zone (default
  `Asia/Hong_Kong`) and a system clock using it.
- Business code that records or compares time takes `Clock`.
- Display code takes `ZoneId` through `ViewFormat`.
- Tests replace the clock with `FixedClockConfiguration` instead of sleeping or
  matching "roughly now".

## Testing seams

- Feature tests use an isolated H2 URL, as the existing suites do.
- Time-dependent tests import `FixedClockConfiguration`.
- Event tests register a recording `AFTER_COMMIT` listener and must not be
  wrapped in `@Transactional`, otherwise the commit never happens.
- Every new behaviour gets a case file under `docs/Test/cases/` and a row in
  [`test-case-catalog.md`](Test/test-case-catalog.md).

## Roadmap this unlocks

Design only; these jobs are not part of the foundation change.

| Job | Where it plugs in | Foundation it uses |
|---|---|---|
| J05 | `history`: wishlist, tracked product, price observation; history listener | after-commit event, Flyway, `Clock` |
| J06 | `alerts`: rules, events, quiet hours, snooze, summary; scheduler | J05 history, `Clock`, settings |
| J07 | `budget`: budget and category totals; extend `Summary` | Flyway, existing list totals |
| J08 | `reminders`: interval/use-rate, next due, snooze, add-to-list | `Clock`/scheduler, J06 |
| J09 | deferred; later `assistant` with checked price facts and a language-model port | stable search/compare views |
| J10 | multi-list refactor plus `transfer`: versioned JSON, preview, import-as-new-list | Flyway, stable list services |
| J11 | `offline`: read-only saved snapshot (HTML/JSON) with saved-at age | stable list views |
| J12 | `preferences`/`privacy`: `MessageSource` i18n, high contrast, export/delete, history views | history data, settings, `Clock` |

Open decisions from [`feasibility-review-simple.md`](feasibility-review-simple.md#5-open-decisions)
(history retention, alert rules, reminder dates, transfer format) must be
settled before the corresponding job, not in this foundation.
