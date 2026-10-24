# J01–J04 UML

These diagrams describe the independent Market Desk baseline for issue #13.
They use the installed `documd-visuals` UML guidance and its **Print** palette.
Labels state meaning without relying on colour. No cloud icons or external
stencils are needed for this local app.

| Question | Editable source | Vector | Image |
|---|---|---|---|
| What can the shopper do? | [Use case](use-case.puml) | [SVG](use-case.svg) | [PNG](use-case.png) |
| Which records and services relate? | [Class](class.puml) | [SVG](class.svg) | [PNG](class.png) |
| How does import-to-restart work? | [Sequence](sequence.puml) | [SVG](sequence.svg) | [PNG](sequence.png) |

## Read the diagrams

The use-case diagram shows shopper goals inside the local app boundary. The
Consumer Council is outside that boundary. Loading the historical sample and
refreshing live prices are separate actions.

The class diagram uses implemented Java entity and service names. It selects
meaningful fields rather than every column. Five embedded `LocalizedLabel`
values carry product name, brand, and three category levels. An offer also embeds
one label value. Shopping items refer to products so a catalogue change does not
delete the user's list. Only one My list and one latest accepted import are used
in this milestone. View records returned by services are not database entities.
The saved retailer choice is nullable: null means automatic lowest. A missing
explicit retailer price stays unknown, with no fallback to another retailer.

The sequence diagram shows manual live import, a rejected-import branch, search,
comparison, and list save/reopen. Repository calls are folded into the H2
lifeline to keep the flow readable. Transactional replacement is implemented by
`CatalogReplacementService`, called through `CatalogService`. A write failure
rolls back the replacement. The saved catalogue is not a price-history series.
Search returns separate ranked Matches and Related groups with category options.
List save/reopen preserves the retailer choice as well as quantity and checked
state. Offers stay visible source text, not calculated discounts.

## Render locally

The app does not need this tool. Diagram export needs Node 18 or later and a
local Chrome/Chromium. The checked exports use `@markdown-viewer/documd` **5.3.2**.
The renderer uses a local loopback browser server. It does not send source to a
public diagram server.

From the repository root on Windows:

```powershell
.\docs\diagrams\baseline\render.ps1
```

Pass `-Chrome 'C:/path/to/chrome.exe'` if Chrome is elsewhere. The script renders
all diagrams by default; use `-Names class,sequence` to regenerate only those
sources when the use-case diagram did not change. The script keeps
the renderer's SVG contents and places them on the Print theme's white ground.
It also flattens transparent PNGs onto white so dark PR previews stay readable.
The `.puml` files are the editing source; regenerate both exports after changes.

In environments that require system certificate trust for npm, use
`$env:NODE_OPTIONS='--use-system-ca'`. This does not turn off certificate checks.
If a sandbox denies the renderer's loopback server, run the same command with
the environment's permitted local-browser access.

The engine is PlantUML-compatible, not the official Java renderer. Do not assume
all official syntax works. Re-render and visually inspect all diagrams after
changing syntax or upgrading the export tool.
