# Market Desk implementation UML through J12

Updated: 19 November 2026. These views describe the current local implementation
after J09–J12. The [J05–J08 set](../j05-j08/README.md) remains unchanged as
historical evidence.

| View | Question answered | Editable source | Export |
|---|---|---|---|
| Use cases | What can the local shopper do, and where do the feed and optional AI participate? | [Source](use-case.puml) | [SVG](use-case.svg) / [PNG](use-case.png) |
| Persisted model | Which records and database relationships survive restart? | [Source](class.puml) | [SVG](class.svg) / [PNG](class.png) |
| Application classes | Which controllers, services, boundaries and repositories implement the flow? | [Source](application-class.puml) | [SVG](application-class.svg) / [PNG](application-class.png) |
| Sequence | How do live import, reminders, checked Ask, history and personal-data actions use stored state? | [Source](sequence.puml) | [SVG](sequence.svg) / [PNG](sequence.png) |

## Reading the views

- The shopper is the only actor who starts a refresh, asks a question, exports a
  file, or deletes personal records. The Consumer Council feed supports an
  explicit refresh. The AI service is optional: it receives checked facts only,
  and every failure falls back to the deterministic checked answer.
- The class view is the persisted data model. J09–J12 adds one table,
  `user_preferences` (locale and contrast). Personal-data export, history, Ask,
  list transfer, and the offline snapshot are views or file outputs, not new
  stored entities. Alerts, observations, list items, reminders, budget, and
  preferences remain the user's rows; products, listings, offers, and accepted
  imports remain source data.
- `ProductEntity.productTargetPrice` and `trackAllRetailers` are personal
  choices stored on a source row. Deleting personal data resets them but keeps
  the accepted product and its source metadata.
- The application view uses actual class names. Dashed arrows show main service
  calls; typed private fields show repository or service dependencies. `HttpAiAnswerClient`
  implements the `AiAnswerClient` boundary. DTOs, Jackson, advice/filters, Clock,
  and transaction-manager plumbing are omitted.
- The sequence shows the import transaction and the separate reminder add, then
  the J09–J12 flows. Ask searches the accepted catalogue first and calls the
  model only for exactly one product. History keeps accepted LIVE observations
  inside 90 days, inserts explicit 7-day gaps, and invents no daily values.
  Export reads personal rows only. Delete requires confirmation, removes those
  rows, and leaves the catalogue and accepted-import metadata in place.
- These views do not claim cloud storage, notifications while the app is closed,
  live list sync, offline editing, or completed wider scope.

## Render and verify

The editable sources are the authority for these exports. Use the official
PlantUML 1.2026.9 renderer. From the repository root:

```powershell
.\docs\diagrams\j09-j12\render.ps1 -Java 'D:/App/Java/JDK21/bin/java.exe'
```

Pass `-PlantUmlJar` for another installation location. The script checks every
source before replacing exports, then produces both SVG and PNG.

On 19 November 2026 the files were rendered with official PlantUML 1.2026.9
(`18d0aae`) and the expected class, boundary, and flow labels were present in
the SVG output. The source relationships were checked against the J12 tests and
implementation contract. A human visual pass over label legibility and connector
balance remains recommended before submission; successful rendering alone is
not a semantic review or an application test.
