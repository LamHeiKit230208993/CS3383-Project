# J01–J04 UML — official PlantUML

Updated: 25 October 2026. These diagrams describe the independent Market Desk
implementation for issue #13. They use the official **PlantUML 1.2026.9 Java JAR**
for final exports and the achromatic Print palette. The `.puml` files are the
editable source; SVG is best for zooming, and PNG is convenient for PR previews.

| Question | Editable source | Vector | Image |
|---|---|---|---|
| What can the shopper do? | [Use case](use-case.puml) | [SVG](use-case.svg) | [PNG](use-case.png) |
| Which persisted records relate? | [Data model](class.puml) | [SVG](class.svg) | [PNG](class.png) |
| How do application classes collaborate? | [Application classes](application-class.puml) | [SVG](application-class.svg) | [PNG](application-class.png) |
| How does import-to-restart work? | [Sequence](sequence.puml) | [SVG](sequence.svg) | [PNG](sequence.png) |

## Meaning and scope

The use-case diagram contains implemented shopper goals only. The Consumer
Council participates in live refresh, not local search or list editing. Category
filtering and retailer selection are shown explicitly. Automatic lowest mode is
available in My list; comparison cards select an explicit priced retailer.
Accounts, shared editing, AI, history, alerts, and offline-phone pages are outside
this milestone. The lines are actor associations, not a build or navigation order.

### Persisted data model

`class.puml` remains the persisted data model. Its landscape layout places record
classes across two rows with short links. Names match the Java entities. Filled
diamonds mark JPA-owned list items, owned offers, and embedded values.
Product-to-listing is a plain association, matching the absence of a JPA cascade.

`ProductEntity` explicitly shows five named `LocalizedLabel` fields: brand, name,
and category1/2/3. `OfferEntity.text` uses the same value type. Its `en`, `zhHant`,
and `zhHans` fields map to English, Traditional Chinese, and Simplified Chinese;
the corresponding JSON keys are `en`, `zh-Hant`, and `zh-Hans`.

The embedded columns are database-nullable. Multiplicities `0..5` and `0..1`
summarize the optional named embeddings; they do not describe a collection or a
separate language table. The parser is stricter: accepted products need all five
labels with three nonblank languages each. Offer text may be blank.

The two-column legend separates database constraints/JPA mapping from parser and
service rules. Uniqueness, foreign keys, nullable columns, and DECIMAL(19,4) are
persistence facts. Quantity 1..999, negative/rounding-price rejection, one My list,
and writing accepted-import ID 1 are application rules, not database CHECKs.

Each shopping item belongs to one list and references one retained product.
`selectedRetailerCode` is a nullable string, not a listing foreign key. Null means
automatic lowest. An explicit choice stays saved even if its current listing or
price disappears. Products absent from a new catalogue become inactive rather
than deleting saved list references. Checked state is not a purchase record.

`AcceptedImportEntity` is an independent application singleton (ID 1), absent
before the first import. It describes the whole accepted catalogue. It has no
foreign key from individual listings and is not a price-history series. Fields
are selective; the implementation guide records the complete rules and interfaces.

### Application classes

`application-class.puml` is the separate application view. It shows the actual
`HelloController`, `CatalogService`, `ShoppingService`, importer/parser/source
client, and all six Spring Data repository interfaces. Selected operations explain
their roles; parameter lists and inherited CRUD operations are abbreviated.

Dashed arrows show the 16 constructor-injected collaborators checked against this
branch's Java code. `PlatformTransactionManager` is a Spring interface;
`ShoppingService` uses it to create its `TransactionTemplate`.
`CatalogJsonParser` uses Jackson's `ObjectMapper`. `LocalRequestGuard` is a
separate filter extending the abstract `OncePerRequestFilter`; it is not a
controller dependency. Solid open triangles show inheritance.

Grouping boxes indicate responsibilities, not exact Java package namespaces.
DTO/view classes, configuration values, bootstrap code, and routine framework
plumbing are omitted. Entity details belong to the data model above. The older
architecture diagram was used only as a size/proportion reference; application
classes and dependencies were verified from the current implementation.

### Main flow

The sequence shows manual sample/live import, full parsing before replacement,
and early exits for download or validation failure. The replacement service's
Spring transaction commits catalogue data and metadata together or rolls back.
Controller notices happen after that transaction. Repository and transaction-
manager calls are folded into H2; the source client is folded into the Consumer
Council endpoint lifeline. Routine common-model reads are omitted.

Search reads the active catalogue and ranks/filters in `CatalogService`, not SQL.
Saving automatic mode stores a null retailer; effective prices are resolved on
list reads. The final phase reuses the same H2 file after restart, reads items
once, and derives `Summary.from(items)`. Startup makes no network request. Offers
remain source text; no multi-buy calculation is implied.

## Regenerate final exports

The app does not need any diagram tool. Exporting this set needs Java, the
**official 1.2026.9 JAR**, and a working Graphviz backend for class/use-case layout.
The verified local backend reports Graphviz 2.44.1. Node, Chrome, and the MCP
server are not required by this export script.

From the repository root on Windows:

```powershell
.\docs\diagrams\baseline\render.ps1
```

The default JAR location is
`$env:USERPROFILE\.codex\tools\plantuml\1.2026.9\plantuml-1.2026.9.jar`.
Use your own installed official JAR and Java paths when needed:

```powershell
.\docs\diagrams\baseline\render.ps1 `
  -PlantUmlJar 'C:/path/to/plantuml-1.2026.9.jar' `
  -Java 'C:/path/to/jdk-21/bin/java.exe'
```

Use `-Names class,application-class` to regenerate the two class views. The default exports all four diagrams. The script checks the JAR
version and every selected source before writing exports, then produces SVG and
PNG through the Java renderer. Invalid syntax stops the run and keeps existing
exports. A later rendering/layout failure can leave a partial set; correct the
failure and regenerate all selected diagrams. Review all outputs after source,
font, Graphviz, or renderer changes; the same PlantUML version can have different
layouts across those environments.

Check Java/Graphviz availability with the JAR's `--version` and
`--check-graphviz` options. The JAR is a local development tool, not bundled in
this repository. Rendering uses local files and local tools, not a public diagram
server. The sources contain no remote includes.

## Renderer distinction and verification

The previous exports used documd's PlantUML-compatible renderer. The replacements
use official PlantUML syntax and Java-renderer exports. `documd-visuals` remains
the source of palette/design guidance; it does not generate these final images.
The optional official PlantUML MCP uses a JavaScript/WASM pipeline. Its syntax
checks provide another check, but its SVG layout is not the Java JAR layout.

On 25 October 2026, all four diagram sources passed the official Java syntax
check, exported as SVG and PNG, and were visually compared with the previous set.
Their types, relationships, and flow were reviewed against current code. The MCP
also accepted all four sources. Script checks covered a path containing spaces
and rejection of invalid source without overwriting an existing SVG.

Pre-remake sources, exports, and script were copied to ignored
`target/uml-remake/previous/` for comparison before replacement. That temporary
backup was removed during cleanup on 25 October 2026. The prior committed set
remains in Git history.
The issue's tutorial-specific method still needs team confirmation: an official
renderer alone does not establish that course requirement.


Before the landscape/class-view update, the diagram set was also copied to
ignored `target/uml-layout-review/previous/` for comparison. That temporary backup
and the intermediate review files were removed during the same cleanup. This
folder contains the accepted UML set, including landscape data-model and
application-class PNGs, editable sources, and SVG exports.
