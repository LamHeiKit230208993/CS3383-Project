# Data and Feasibility Review

Updated: 17 October 2026.

Status: Document review. Live data and implementation checks remain incomplete.

The [project scope](project-scope.md) is the main record of team decisions. This file explains feasibility; it does not add, remove, or defer selected features.

## 1. Overall assessment

The planned app is feasible in principle, subject to data checks and clear feature rules. Search, comparison, saved items, lists, and user-set reminders have a clear basis. Chat must use checked price results.

The selected scope also includes budgets, shared lists, three languages, high-contrast display, and offline use. These need real work and testing. They are not optional merely because a small comparison demo can work without them.

Conditional features need specific evidence. Price history requires dated records. Actual savings require purchases and a stated comparison price. Future-price advice needs a tested method.

No reliable completion date or cost can be given until the team records its deadline, available time, tool choices, and course requirements. The course focus on AI in software engineering is confirmed by the team; the detailed marking guide has not been supplied.

## 2. Evidence and data limits

The [official dataset page](https://data.gov.hk/en-data/dataset/cc-pricewatch-pricewatch) was checked during the earlier review on 16 October 2026. It listed daily updates, a JSON resource, CSV resources in three languages, and a historical-download section.

The current CSV and JSON contents were not retrieved successfully in that review. The web tool could not read the CSV and timed out on JSON; a direct request failed to resolve the host name. This document update did not repeat those requests.

Therefore, the following field description comes from the earlier local analysis, not a fresh file check:

| Reported field | Possible use | Check required |
|---|---|---|
| Product code | Match records across retailers | Verify code stability and exact product/pack matches |
| Brand and product name | Search and display | Check language, pack descriptions, and inconsistent names |
| Three category fields | Browse and group products | Check missing and inconsistent categories |
| Retailer code | Identify a store company | Confirm code meanings; do not infer branches |
| Price | Compare listed amounts | Check currency, missing values, and offer treatment |
| Offer text | Show conditions from the source | Retain original text; calculate only understood offer types |

The earlier analysis reports no separate fields for regular price, structured pack size, delivery charges, stock, branch locations, item purchase URLs, or offer dates. Verify this against the selected resource before implementation. Do not assume CSV and JSON have identical fields.

The current record count, retailer count, duplicate rate, source-date fields, and historical coverage are unverified. No current price-service connection, AI answer, alert delivery, or running cost has been tested here.

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
| Features are easy because they are “app-side” | Shared lists, offline edits, user access, and notifications each need defined rules and tests. |

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
| Shared lists | Respect access and avoid silent loss of changes | Two users editing the same item get the agreed result |
| Offline use | Show saved-data age and supported actions | No connection must not be presented as a fresh price or successful remote chat |

These are acceptance checks, not results from completed tests. Made-up data may test a rule, but it does not verify live data access.

## 5. Open decisions

Open decisions do not make planned features optional. They define the remaining detail needed to build them consistently.

| Topic | Decision still needed | Related scope |
|---|---|---|
| Tracking | Are wishlist and tracked items separate? Does adding a shopping-list item start tracking? One retailer or all? What happens on removal and re-addition? | R10, R14, R21–R22, R35, R39 |
| History and dates | Start history at tracking time or include older records? How much data is retained? What source/date label appears on untracked results? | R10, R14, R44 |
| Alert rules | At or below target, or strictly below? Which earlier price is used? Alert immediately if already met? When can the same price trigger again? | R21–R24 |
| Morning summary | Which products, delivery channel, time, and quiet-hours behaviour? | R23, R26 |
| Spending | Planned costs, actual purchases, or both? How are purchases entered? Which comparison price defines savings? | R18, R30–R31, R43 |
| Shared and offline lists | Who can view/edit? How are simultaneous edits handled? Offline viewing only, or editing too? | R32, R45, R48 |
| Advice and offers | What measured rule supports conditional advice or price comparisons? What happens without enough records? | R11, R16, R20, R35 |
| Reminders | User-set interval or recorded use rate? How does a purchase or snooze change the next date? | R34, R36–R37 |
| Project limits | Course marking guide, deadline, team availability, operating budget, and chosen tools | All planned work |

## 6. Evidence needed before stronger claims

Retrieve and inspect a real file. Check the reported fields, duplicate records, missing prices, product matches, offer text, and language joins. Check historical downloads separately. Confirm source credit and usage terms before release.

Measure AI answer correctness, response time, notification delivery, and operating cost with the selected services. Record observed values; do not replace missing measurements with estimates presented as facts.

The main cost categories are hosting, storage, AI calls, and external messages. No provider price or free-service limit is assumed here.

## Sources

- [Team feature decisions](project-scope.md)
- [Product description and user-needs links](product-description.md)
- [Build options](architecture-options.md)
- [Official dataset page](https://data.gov.hk/en-data/dataset/cc-pricewatch-pricewatch)
- [English CSV](https://online-price-watch.consumer.org.hk/opw/opendata/pricewatch_en.csv) and [JSON resource](https://online-price-watch.consumer.org.hk/opw/opendata/pricewatch.json): linked by the source page; contents not verified in the earlier retrieval attempt.
