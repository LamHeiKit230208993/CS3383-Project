# Project Scope and Team Decisions

Updated: 17 October 2026.

Status: Reflects the team's manual feature review. Implementation and data checks are not complete.

This is the main record of feature decisions. The [feasibility review](feasibility-review-simple.md) records data limits. The [product description](product-description.md) defines shared terms. Earlier suggestions do not override this scope.

## 1. How to read the decisions

| Original mark | Meaning |
|---|---|
| + | Build this feature. Keep the stated technical limits. |
| # or ex | Future consideration only. It may not be built. |
| ! | Applies only to items the app user adds for tracking. Keep the emoji's condition. |
| Unmarked ✅ | Include the feature. |
| Unmarked ❌ | Exclude the feature. |
| Unmarked ⚠️ | Conditional: keep the stated limits and resolve the required information before committing to unsupported behaviour. |

The # mark above refers to a mark in a feature row, not a Markdown heading.

The tables preserve all 48 original feature rows and their marks. R01–R48 are reference IDs, not build order. “Build” records team intent; it is not a claim that the data or feature has passed a test. “Tracked” applies the ! limit.

## 2. Search and comparison

| ID | Feature | Original mark | Decision and boundary |
|---|---|---|---|
| R01 | Search by product, brand, category, or retailer; handle spelling differences | ✅ + | Build. Test English and Chinese search; language files alone do not provide spelling correction. |
| R02 | Compare the same product across retailers | ✅ + | Build. Compare listed records for the same product and pack. Show ties and missing listings. |
| R03 | Rank by price per gram, litre, or piece | ❌ | Excluded. Do not add this merely because some names contain a usable size. |
| R04 | Match product records with different names | ⚠️ | Conditional. Use product codes as a starting point; check brand, type, and pack. Different pack sizes must not become one exact-match item. |
| R05 | Delivery versus in-store prices, fees, and minimums | ❌ | Excluded. The reported fields do not supply these details. |
| R06 | Compare complete shopping-list totals across stores | ⚠️ | Conditional. Define the same items and quantities, handle missing prices, and state which costs are excluded. Distinct from summing per-item choices in R28. |
| R07 | Mark items out of stock or discontinued | ❌ | Excluded. A missing listing does not establish either state. |
| R08 | Product quality or review scores | ❌ | Excluded. No supporting source has been established. |

## 3. Price history and offers

| ID | Feature | Original mark | Decision and boundary |
|---|---|---|---|
| R09 | Regular price, sale price, and verified discount percentage | ❌ | Excluded. A listed price or offer claim is not a verified regular price. |
| R10 | Price history charts for 30 or 90 days | ⚠️ ! | Tracked; conditional on dated records. Show only the available period. Older downloads may help if checked. |
| R11 | Check advertised discounts, originally called “fake-discount detection” | ⚠️ | Conditional. Compare recorded prices and explain the result. History alone cannot prove a fake offer or intent. |
| R12 | Offer age and end date | ❌ | Excluded. Do not invent dates from the first or last time the app saw an offer. |
| R13 | Mark an offer as expired | ❌ | Excluded. Removal from a daily file does not prove expiry. |
| R14 | Show source and last-update information | ✅ ! | Build for tracked items. Distinguish the source date, when known, from the app's retrieval time. |

## 4. Chat and advice

| ID | Feature | Original mark | Decision and boundary |
|---|---|---|---|
| R15 | Ask a price question and receive a recommendation with a reason | ⚠️ + | Build. Answer from checked records; identify product, pack, retailer, and listed price. Explain ties or missing data. |
| R16 | Buy-now or wait advice | ⚠️ | Conditional. Historical comparison is possible with records. A future-price claim needs a defined method and separate tests. |
| R17 | Bulk-buy advice based on storage space | ❌ | Excluded. Pack, quantity, and household information are not established. |
| R18 | Clarify an unclear request; use past purchases where available | ⚠️ + | Build. Ask for a product choice when unclear. Past-purchase defaults need recorded purchases; saved items are not proof of purchases. |
| R19 | Voice input and answers within five seconds | ⚠️ ex | Future consideration. Neither voice nor this response-time target is a current commitment. |
| R20 | Report when earlier wait advice was wrong | ⚠️ | Conditional on R16. Needs saved advice, its date, a defined comparison period, and later prices. |

## 5. Alerts

| ID | Feature | Original mark | Decision and boundary |
|---|---|---|---|
| R21 | Target-price alerts and price-drop alerts | ⚠️ + | Build as two checks. A target needs a current price; a drop needs an earlier price. Check after valid daily updates. |
| R22 | Amount or percentage change thresholds | ⚠️ ! | Tracked; conditional on a stated comparison price. A percentage change is not a verified store discount. |
| R23 | Quiet hours, prevention of repeated alerts, and snooze | ✅ + | Build. Define when a repeated price or a new tracking period can trigger another alert. |
| R24 | Different notification channels | ⚠️ | Conditional. Choose and test the channels; daily data does not support live flash-sale alerts. |
| R25 | Alerts near a store branch | ❌ # | Future consideration. Branch location data is not established. |
| R26 | Morning summary | ✅ + | Build. Agree which items it covers, its delivery channel, and its time. |

## 6. Lists and spending

| ID | Feature | Original mark | Decision and boundary |
|---|---|---|---|
| R27 | Save recurring shopping lists and check their prices again | ✅ + | Build. Store chosen products and quantities. |
| R28 | Find the lowest listed price for each item and add a list total | ⚠️ + | Build. Identify each retailer; several store visits may be needed. Missing prices make the total incomplete. |
| R29 | Track changes in a fixed shopping-list cost | ⚠️ | Conditional on comparable dated records. Keep items and quantities fixed. This is not a measure of general inflation. |
| R30 | Weekly/monthly budgets, totals, and warnings | ✅ + | Build. Label planned cost separately from actual spending. Actual spending needs purchase entries. |
| R31 | Spending by category | ✅ + | Build. State whether the chart uses planned cost or recorded purchases. |
| R32 | Shared lists, updates, and handoff between shoppers | ✅ + | Build. Define access, simultaneous edits, and who is shopping. |
| R33 | Flag larger packs as better value | ❌ | Excluded. This depends on unit-price comparison. |

## 7. Reminders and saved items

| ID | Feature | Original mark | Decision and boundary |
|---|---|---|---|
| R34 | Reminders based on use | ✅ + | Build. Obtain a user-set use rate or interval; price data cannot show household consumption. |
| R35 | Buy-now or wait advice in reminders | ⚠️ ! | Tracked; conditional. Apply R16's evidence limits. This does not create a separate permission to predict prices. |
| R36 | Add a reminder item to a list with a listed-price comparison | ✅ + | Build. Keep the chosen product and quantity clear. |
| R37 | Clear or snooze a reminder | ✅ + | Build. Define the next reminder date after each action. |
| R38 | Link to a purchase page | ⚠️ | Conditional. A verified retailer link is possible; an item page needs a verified item URL. No checkout is implied. |
| R39 | Save to a wishlist | ✅ + | Build. Define its relation to the tracked-items list; saving does not record a purchase. |
| R40 | Share a result to WhatsApp; copy a coupon code | ⚠️ | Conditional, with separate actions. Sharing needs a usable result. Copying a coupon needs an actual code from a checked source. |
| R41 | Online-only, in-store-only, or member-only eligibility labels | ❌ | Excluded. No reliable structured source has been established. |
| R42 | Buy inside the app or get a walking route | ⚠️ # | Future consideration. Needs store checkout support or branch/map data. |

## 8. User controls and access

| ID | Feature | Original mark | Decision and boundary |
|---|---|---|---|
| R43 | Money saved this month | ⚠️ | Conditional. Actual savings need purchases and a stated comparison price. Otherwise show an estimated price difference. |
| R44 | Price change since last week/month | ⚠️ | Conditional on comparable dated records. Show gaps and the dates used. |
| R45 | View, export, and delete stored personal data | ✅ + | Build. Define which records belong to each user, including shared-list records. |
| R46 | English, Traditional Chinese, and Simplified Chinese | ✅ + | Build. Cover app text and product display; check search behaviour in each language. |
| R47 | High-contrast, easy-to-read mode | ✅ + | Build. Check readable text, clear controls, and meaning that does not rely only on colour. |
| R48 | Offline use of saved data | ✅ + | Build. Show the saved data's age. Decide separately whether offline editing is supported. |

## 9. Rules for every included feature

- Use valid source records. Missing prices are unknown, not zero.
- “Lowest listed price” means the lowest valid price among the compared listings for the selected product and pack.
- Keep the original offer text. Calculate only offer types with checked rules and quantities.
- Preserve the last good data if an update fails. A new download time does not prove a new price observation.
- Price calculations must use fixed app rules. AI must not invent prices, stock, dates, or purchase history.
- Treat product and offer text as data, not instructions to the AI.
- Do not silently broaden tracked-only features or move excluded/future features into the build.
- Shared and offline features are planned work, not optional extras. Their exact behaviour still needs decisions.

Unresolved choices are listed once in the [feasibility review](feasibility-review-simple.md#5-open-decisions).
