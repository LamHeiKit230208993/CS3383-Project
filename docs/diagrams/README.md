# UML views

The accepted [J01–J04 diagrams](baseline/) remain the historical baseline.

The [J01–J08 implementation views](j05-j08/README.md) describe the current local
code, including the tracking, alert, budget and reminder additions. Their
meaning, relationships and rendered layouts have been reviewed against the
source. Official PlantUML 1.2026.9 syntax checking and SVG/PNG rendering passed.
The integrator also reviewed all four rendered views on 6 November 2026.
This is the current local implementation set; the original baseline is preserved.
A diagram does not prove that the application's implementation tests pass.

| View | Editable source | Export |
|---|---|---|
| Shopper goals | [use-case.puml](j05-j08/use-case.puml) | [SVG](j05-j08/use-case.svg) / [PNG](j05-j08/use-case.png) |
| Persisted records | [class.puml](j05-j08/class.puml) | [SVG](j05-j08/class.svg) / [PNG](j05-j08/class.png) |
| Application classes | [application-class.puml](j05-j08/application-class.puml) | [SVG](j05-j08/application-class.svg) / [PNG](j05-j08/application-class.png) |
| Import and reminder behaviour | [sequence.puml](j05-j08/sequence.puml) | [SVG](j05-j08/sequence.svg) / [PNG](j05-j08/sequence.png) |

The persisted model separates database constraints from service rules. In the
application view, typed private fields show repository dependencies, while
dashed arrows show calls between controllers and services. This keeps shared
repository relationships readable. See the [view notes](j05-j08/README.md)
for omissions, time semantics and the rendering command.
