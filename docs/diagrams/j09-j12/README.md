# Market Desk UML for J09–J12

Updated: 18 November 2026. These views describe the J09–J12 code on
`eric/final-enhancement`. The earlier [J01–J04](../baseline/README.md) and
[J05–J08](../j05-j08/README.md) sets are unchanged as historical evidence.

| View | Question answered | Editable source | Export |
|---|---|---|---|
| Use cases | What can the local shopper do in J09–J12, and where do the feed and AI participate? | [Source](use-case.puml) | [SVG](use-case.svg) / [PNG](use-case.png) |
| Records | Which records are the user's personal data, and which are source data? | [Source](class.puml) | [SVG](class.svg) / [PNG](class.png) |
| Application classes | Which controllers and services implement the flows? | [Source](application-class.puml) | [SVG](application-class.svg) / [PNG](application-class.png) |
| Sequence | How do language, contrast, personal-data, and history requests flow? | [Source](sequence.puml) | [SVG](sequence.svg) / [PNG](sequence.png) |

## Reading the views

- The use-case actor is the one local shopper. The DeepSeek model explains
  checked facts only; it never supplies a price.
- The records view splits the user's data from the imported source catalogue.
  `/my-data/export` and `/my-data/delete` cover only the user's side.
- `observedAt` is the app's retrieval time, because the feed has no source price
  date; `/history` shows gaps instead of inventing a daily series.
- Language and contrast choices are first-party cookies (`LANG`, `CONTRAST`),
  not server sessions, so they survive a restart.

## Rendering

`render.ps1` uses the official PlantUML 1.2026.9 JAR, checks each source's
syntax, then writes SVG and PNG. All four views were regenerated and reviewed
on 18 November 2026.
