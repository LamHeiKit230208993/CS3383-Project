# J05–J08 implementation summary

Updated: 12 November 2026.

This document summarises the change set that moves Market Desk from the merged
J01–J04 baseline to a code-complete J05–J08 state (wishlist and tracking,
planned budgets, in-app alerts, and replenishment reminders). It complements the
[milestone plan](Plan/tracking-alerts-budgets-reminders-j05-j08.md) and the
[test history](Test/history.md). J05–J08 are **not closed** until the combined
acceptance flow and the integrated browser check are recorded.

## Agreed decisions (D01–D07)

The decision register was resolved on 12 November 2026 and recorded in the
[milestone plan](Plan/tracking-alerts-budgets-reminders-j05-j08.md#decisions-before-dependent-work):

| ID | Rule |
|---|---|
| D01 | Track every priced retailer listing of a tracked product; enabling tracking is explicit, not auto-seeded. |
| D02 | History lives in separate tables, survives catalogue replacement; untracking stops collection, keeps history. |
| D03 | One observation per (product, retailer) per accepted import; unchanged repeated file adds none; drop uses most recent distinct prior price. |
| D04 | Target fires at or below; drop needs a distinct earlier observation; repeat prevention per observation. |
| D05 | One `Asia/Hong_Kong` clock; summary 08:00; quiet hours 22:00–07:00; 8-hour event snooze; no scheduler. |
| D06 | Single weekly/monthly budget vs live known subtotal; category totals reconcile; no rollover. |
| D07 | Interval + configurable snooze; clear deactivates; add-to-list does not change reminder state. |

## Backend changes

New packages under `com.pricewatch`:

- `tracking/` — `TrackingService`, `ObservationRecorder`, and entities
  `WishlistEntryEntity`, `TrackingChoiceEntity`, `PriceObservationEntity` with
  their repositories and `TrackedProductView`. Observations are keyed by
  product/retailer strings, never by listing foreign keys, so catalogue
  replacement cannot delete history. Each observation keeps an offer snapshot.
- `budget/` — `BudgetService`, `BudgetSettingsEntity`, repository, `BudgetView`.
  Category totals use `category2` → `category1` → "Uncategorised" and reconcile
  with the known subtotal.
- `alerts/` — `AlertService`, `NotificationPolicy`, `AlertRuleEntity`,
  `AlertEventEntity`, repositories, `AlertPageView`. Target/drop evaluation,
  repeat prevention, quiet hours, snooze, and a morning summary.
- `reminders/` — `ReminderService`, `ReminderEntity`, repository, `ReminderView`.
  Interval and snooze in days; add-to-list reuses `ShoppingService.add`.
- `config/ClockConfig` — a single injectable `Clock` for all time behaviour.
- `shopping/EffectivePrice` — the shared effective-price rule extracted from
  `ShoppingService`.

Modified source:

- `CatalogService` — after a committed import, records observations and evaluates
  alerts; a history-recording failure is reported, never rolls back the accepted
  import; `accept` is now public.
- `ShoppingService` — `toView` uses `EffectivePrice`.
- `HelloController` — routes for `/saved`, `/tracking/*`, `/budget`,
  `/alerts/*`, and `/reminders/*`; Saved and Alerts left the Planned set.

## Templates and styling

- New pages: `pages/saved.html`, `pages/alerts.html`, `pages/reminders.html`.
- Updated: `pages/list.html` (budget + category totals), `pages/compare.html`
  (save/track actions), `market-desk.html` (scene wiring), `navigation.html`
  (Saved, Alerts, Reminders become real links; only Ask stays Planned), and
  `market-desk.css`.

## Tests

All automated Java tests pass: **76 tests, 0 failures** (previously 47).

- New: `TrackingServiceTest` (8), `BudgetServiceTest` (6), `AlertServiceTest`
  (7), `ReminderServiceTest` (5). Alert and reminder tests use a fixed
  `Asia/Hong_Kong` clock.
- Updated: `HelloControllerTest`, `HelloControllerModelTest`,
  `CatalogServiceIntegrationTest`, `SearchRelevanceTest`, `ShoppingServiceTest`
  (new-table cleanup), and the browser script `market-desk.cjs` (Saved now opens
  `/saved`).

## Documentation and UML

- `Plan/tracking-alerts-budgets-reminders-j05-j08.md`: decisions filled.
- `Plan/features.md`, `SETUP.md`, `README.md`, `Test/test-case-catalog.md`,
  `feasibility-review-simple.md`: updated for the new milestone state.
- New case documents: `Test/cases/TC-TRACK-001.md`, `TC-BUDGET-001.md`,
  `TC-ALERT-001.md`, `TC-REMINDER-001.md`.
- `Test/history.md`: two dated entries (implementation run and UML regeneration).
- All four PlantUML sources updated and regenerated to SVG/PNG with the official
  PlantUML 1.2026.9 JAR.

## Outstanding work before closing J05–J08

1. Run the browser suite (`npm run test:browser`) against the rebuilt JAR and
   record the result.
2. Record the combined acceptance flow (milestone checklist items 1–7).
3. Visually review the regenerated PNG diagrams.
