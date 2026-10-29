# Data and Feasibility Review

Updated: 29 October 2026. The source-inspection observations below remain dated 17 October 2026.

Status: Source inspection was completed on 17 October 2026. J01–J04 implementation checks have since passed; see the [test catalogue](Test/test-case-catalog.md) for dated evidence. The wider app scope and later jobs remain incomplete.

The [project scope](project-scope.md) is the main record of team decisions. This file explains feasibility; it does not add, remove, or defer selected features.

## 1. Overall assessment

The planned app is feasible in principle, subject to data checks and clear feature rules. Search, comparison, saved items, lists, and user-set reminders have a clear basis. Chat must use checked price results.

The selected scope also includes budgets, list export/import, three languages, high-contrast display, and offline use. The app has one local user, no accounts, and no cloud saving. Each imported list is a separate copy. These selected features still need work and testing.

Spring Boot is chosen. Normal storage must survive restart. First alerts/reminders appear inside the active app; first offline use is read-only saved-list review with live price refresh and remote chat disabled.

Conditional features need specific evidence. Price history requires dated records. Actual savings require purchases and a stated comparison price. Future-price advice needs a tested method.

No reliable completion date or cost can be given until the team records its deadline, available time, tool choices, and course requirements. The course focus on AI in software engineering is confirmed by the team; the detailed marking guide has not been supplied.

## 2. Evidence and data limits

The [official dataset page](https://data.gov.hk/en-data/dataset/cc-pricewatch-pricewatch) was checked during the earlier review on 16 October 2026. It listed daily updates, a JSON resource, CSV resources in three languages, and a historical-download section.

The 16 October review could not retrieve the source files. A direct JSON download succeeded on 17 October 2026. It contained 2,736 products, 7,818 retailer price entries, 2,482 offer entries, and nine retailer codes in a file of 2,741,295 bytes. These are counts for one download, not fixed provider limits.

The [JSON sample check](Plan/data-sample-review.md) records the fields, checks, file hash, two saved source records, and remaining limits. CSV contents and historical downloads were not checked in this update.

| Observed JSON field | Possible use | Check still required |
|---|---|---|
| `code` | Keep language labels and retailer prices under one product | Verify cross-day stability and exact product/pack matches |
| `brand`, `name` | Search/display labels in `en`, `zh-Hant`, and `zh-Hans` | Test spelling tolerance, mixed queries, and aliases not supplied by the source |
| `cat1Name`, `cat2Name`, `cat3Name` | Browse/group using multilingual labels | Check category meanings before category-level alerts |
| `prices[].supermarketCode` | Identify a store company | Confirm code meanings; do not infer branches |
| `prices[].price` | Parse listed price strings as decimals | Check currency and offer treatment; test missing/invalid future input |
| `offers[]` | Keep retailer-linked offer text in three languages | Preserve quantities; calculate only understood offer types |

This download had no duplicate product codes, no missing name label in the three languages, and no price value that failed decimal parsing or was negative. This does not prove that future files have the same quality.

The product fields did not supply separate regular prices, structured pack sizes, delivery charges, stock, branch locations, purchase URLs, offer dates, or a source-date field. Do not assume CSV and JSON have identical fields. Retrieval time must not be presented as the source price date.

Full-catalogue history is not proved impossible. The baseline keeps only selected products' older price records to limit the first implementation. The checked endpoint returned the whole catalogue; smaller filtered downloads were not established. Database growth, historical coverage, search behaviour, AI answers, and alert delivery remain untested.

## 3. Corrections to the earlier analysis

| Earlier claim | Correct rule |
|---|---|
| All price alerts need history | A target-price check needs one current price. A price-drop check needs an earlier comparison price. |
| History is available only after months of running the app | Checked historical downloads may supply older records. Otherwise collect daily records and show the actual coverage. |
| Unit prices are impossible | Some clear pack names may support a calculation. General reliability is unverified, and R03 remains excluded by the team. |
| Historical prices can detect a fake discount | They can show a price difference. They do not prove deception or establish every offer condition. |
| A price trend is enough to advise waiting | A future-price claim needs a defined method and tests against later observations. |
| Savings can only be calculated from our own history | A stated alternative price can provide a comparison. Actual savings still require purchase quantities and amounts. |
| Product codes solve product matching | Codes help, but exact product and pack matching must be checked. |
| Features are easy because they are “app-side” | List imports, offline edits, and notifications each need defined rules and tests. |
| All-product history is impossible, but selected history is possible | One catalogue download succeeded. Neither long-term storage limit has been measured. Selected tracking is a baseline design choice; it does not prove that the provider supports smaller downloads. |

These corrections explain technical limits. They do not change the team's Build, Conditional, Excluded, or Future decisions.

## 4. Required behaviour and checks

| Area | Required behaviour | Example check |
|---|---|---|
| Matching | Compare the same product and pack | A 2 kg pack must not be treated as the same listing as a 5 kg pack |
| Missing data | Keep unknown values separate from valid prices | A missing price must not become the cheapest result |
| Offers | Preserve conditions and required quantities | “2 for HKD 50” requires two qualifying packs; it is not HKD 25 for one pack |
| Totals | Use quantities and report missing items | A store missing an item cannot win through an incomplete total |
| Dates | Separate source date from retrieval time | Downloading an old file today must not create a new price observation |
| Updates | Keep the last good data when an import fails | A failed or empty response must not erase valid listings |
| Chat | Use checked results and handle unknowns | Every amount in the answer matches a source record; an unclear product triggers clarification |
| Tracked items | Apply ! only to explicitly tracked items | Define the effect of adding, removing, and re-adding an item |
| List sharing | Export and import a separate local copy | Preserve items, quantities, and checked state; reject invalid input; leave existing lists unchanged; later edits do not sync |
| Local storage | Preserve saved records across app restarts | Save a list and settings, restart the normal app, and read them again |
| Active-app jobs | Run any agreed scheduled work only while the local app runs | A closed app does not claim collection or delivery. Current imports are manual; startup checks and scheduling remain proposed until milestone decision D05 is resolved |
| Offline use | Read a previously saved list on the viewing device; show saved-data age | Disconnect and reopen the saved list; price refresh and remote chat remain off |

These are acceptance checks, not results from completed tests. Made-up data may test a rule, but it does not verify live data access.

## 5. Open decisions

Open decisions do not make planned features optional. They define the remaining detail needed to build them consistently.

For the agreed J05–J08 milestone, use the decision register in the
[execution plan](Plan/tracking-alerts-budgets-reminders-j05-j08.md#decisions-before-dependent-work).
Record agreed answers there and link them from this overview. A milestone issue
does not by itself resolve the questions below or approve conditional features.

| Topic | Decision still needed | Related scope |
|---|---|---|
| Tracking | Use explicit tracking, separate from saving a wishlist or list. Before the tracking job, set retailer selection, retained records, and re-addition rules. | R10, R14, R21–R22, R35, R39 |
| History and dates | Start history at tracking time or include older records? How much data is retained? What source/date label appears on untracked results? | R10, R14, R44 |
| Alert rules | At or below target, or strictly below? Which earlier price is used? Alert immediately if already met? When can the same price trigger again? | R21–R24 |
| Morning summary | Which products, time, quiet-hours rule, and missed-run behaviour? First delivery is inside the active app. | R23, R26 |
| Spending | Planned costs, actual purchases, or both? How are purchases entered? Which comparison price defines savings? | R18, R30–R31, R43 |
| Saved-list viewing | How is the list/page saved on the viewing device? First offline use is read-only, with no live price refresh or remote chat. | R48 |
| Advice and offers | What measured rule supports conditional advice or price comparisons? What happens without enough records? | R11, R16, R20, R35 |
| Reminders | User-set interval or recorded use rate? How does a purchase or snooze change the next date? | R34, R36–R37 |
| Project limits | Course marking guide, deadline, team availability, and operating budget. Spring Boot is chosen; other frameworks are backups. | All planned work |

## 6. Evidence needed before stronger claims

Actual sample/live app imports and saved-data reopening have passed the J01–J04 checks. Automated tests cover stored source labels, separate pack codes, malformed input, original offer text, and rollback after a database write failure. See the [test catalogue](Test/test-case-catalog.md) for observed results. Cross-day source-code stability and cross-code product matching remain unverified. Check historical downloads separately, validate rules before calculating any offer, and confirm source credit and usage terms before release.

Measure AI answer correctness, response time, notification delivery, and operating cost with the selected services. Record observed values; do not replace missing measurements with estimates presented as facts.

The first baseline runs locally. Measure local storage and AI-call costs; hosting and external-message costs apply only if those future services are added. No provider price or free-service limit is assumed here.

## Sources

- [Team feature decisions](project-scope.md)
- [Product description and user-needs links](product-description.md)
- [Build options](architecture-options.md)
- [Official dataset page](https://data.gov.hk/en-data/dataset/cc-pricewatch-pricewatch)
- [JSON resource](https://online-price-watch.consumer.org.hk/opw/opendata/pricewatch.json): downloaded and inspected on 17 October 2026; see the [sample check](Plan/data-sample-review.md).
- [English CSV](https://online-price-watch.consumer.org.hk/opw/opendata/pricewatch_en.csv): alternative resource; contents not checked in this update.
