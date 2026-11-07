# J05-J08 UML - tracking, alerts, budgets, reminders

Updated: 31 October 2026. These diagrams extend the accepted J01-J04 baseline
(`docs/diagrams/baseline/`, kept intact as evidence) with the J05-J08 records and
flows. The `.puml` files are the editable source; SVG is best for zooming and PNG
for PR previews.

| Question | Editable source | Vector | Image |
|---|---|---|---|
| What can the shopper do now? | [Use case](use-case.puml) | [SVG](use-case.svg) | [PNG](use-case.png) |
| Which persisted records relate? | [Data model](class.puml) | [SVG](class.svg) | [PNG](class.png) |
| How do application classes collaborate? | [Application classes](application-class.puml) | [SVG](application-class.svg) | [PNG](application-class.png) |
| How does import -> record -> alert -> show, and reminder add work? | [Sequence](sequence.puml) | [SVG](sequence.svg) | [PNG](sequence.png) |

## What changed from the baseline

- **Use case**: added shopper goals for save (wishlist), track/untrack, view
  in-app alerts, set a planned budget, and manage reminders. Background
  device/cloud push, accounts, and cloud storage stay out of scope.
- **Data model**: added `WishlistEntryEntity`, `TrackingChoiceEntity`,
  `PriceObservationEntity`, `AlertRuleEntity`, `AlertEventEntity`,
  `BudgetSettingsEntity`, and `ReminderEntity`. Observations copy product/retailer
  codes and never point at deleted listing rows.
- **Application classes**: added `TrackingService`, `AlertService`,
  `BudgetService`, `ReminderService`, and `PostImportProcessor`, with their
  repositories and the injected `Clock`.
- **Sequence**: added `import -> record observation -> evaluate alert -> show
  event`, the reminder add-to-list path, and in-app alert viewing.

## Regenerate

```powershell
.\docs\diagrams\j05-j08\render.ps1
```

The script defaults to the VS Code PlantUML extension JAR
(`$env:USERPROFILE\.vscode\extensions\jebbs.plantuml-2.18.1\plantuml.jar`,
version 1.2024.3) so no system Graphviz is required; it prints a warning when the
version is not 1.2026. The JAR is a local development tool, not bundled here.
