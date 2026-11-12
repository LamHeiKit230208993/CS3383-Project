# Market Desk implementation UML through J08

Updated: 6 November 2026. These views describe the current local implementation.
The integrator reviewed the source relationships and all four rendered views
against the locally tested implementation on that date. The original
[J01–J04 set](../baseline/README.md) remains unchanged as historical evidence.

| View | Question answered | Editable source | Export |
|---|---|---|---|
| Use cases | What can the local shopper do, and where does the feed participate? | [Source](use-case.puml) | [SVG](use-case.svg) / [PNG](use-case.png) |
| Persisted model | Which records and database relationships survive restart? | [Source](class.puml) | [SVG](class.svg) / [PNG](class.png) |
| Application classes | Which controllers, services, parser and repositories implement the flow? | [Source](application-class.puml) | [SVG](application-class.svg) / [PNG](application-class.png) |
| Sequence | How do a live update, daily summary and manual reminder add affect stored state? | [Source](sequence.puml) | [SVG](sequence.svg) / [PNG](sequence.png) |

## Reading the views

- The use-case actor on the left is the shopper. The Consumer Council feed on
  the right supports a refresh. Association lines do not mean that the feed
  starts an import or sends an alert.
- The class view is the persisted data model, not the application's service
  architecture. A `LocalizedLabel` is an embedded value with English,
  Traditional Chinese and Simplified Chinese fields; it has no separate table.
  The legend distinguishes database mappings and constraints from service rules.
- An alert can refer to a tracking period, but `observationId` is a saved scalar
  identifier, not a foreign key. Drop events retain both compared prices and
  their retrieval times. These are distinct from alert generation time.
  A target can use a fresh LIVE current price without creating another history
  observation. Its nullable observation ID does not supply a uniqueness guard;
  armed state and tracking-period locking prevent repeats in that case.
- The application view uses actual class and repository names. Typed private
  fields show repository dependencies; dashed arrows show the main service
  calls. A repository type need not be expanded into another box to show this
  relationship. All these repository interfaces extend Spring Data
  `JpaRepository`.
- Key operations are abbreviated. Framework filters/advice, DTOs, inherited CRUD,
  Jackson, the shared Clock, transaction-manager plumbing and common formatting
  dependencies are described in the legend rather than expanded into more
  crossing connectors. Responsibility groups are not exact Java packages.
- The sequence groups TrackingService and AlertService on one lifeline. It
  focuses on the shared import transaction and the separate ShoppingService
  transaction used for a manual reminder add. A later display or summary
  failure cannot undo an already committed import.
- These views make no claim of cloud storage, delivery while the app is closed,
  purchase accounting, calculated promotional prices, or completed wider scope.

## Render and verify

The editable sources are the authority for these exports. Use the official
PlantUML 1.2026.9 renderer. From the repository root:

```powershell
.\docs\diagrams\j05-j08\render.ps1 -Java 'D:/App/Java/JDK21/bin/java.exe'
```

Pass `-PlantUmlJar` for another installation location. The script checks every
source before replacing exports, then produces both SVG and PNG. After a source
change, inspect all affected rendered views for readable labels, relationships,
connector crossings and balance. Successful rendering alone is not a semantic
review or an application test.
