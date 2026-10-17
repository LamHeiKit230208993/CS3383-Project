# Baseline Feature and Job Plan

Updated: 17 October 2026.

Read the [project scope](../project-scope.md), [baseline decisions](baseline-proposal.md), and [JSON sample check](data-sample-review.md) first. The scope decides what is included. This file sets the build order and completion checks. A plan entry is not evidence that a feature works.

## 1. First milestone

Build this small working flow:

**Import checked prices -> search -> select the exact product -> compare retailer prices -> add to a shopping list -> restart and read the saved list.**

Build a simple screen with each step. Do not wait for a complete visual design before testing real data. Do not build every screen with placeholder data and call the baseline complete.

The first milestone uses Spring Boot and a local H2 file database. No accounts, cloud saving, AI call, or notification service is needed to prove this flow. Those included features follow in later jobs.

## 2. Shared rules for all jobs

- Use R01-R48 as the scope references. F1-F25 below keep earlier plan references usable; their scope marks come from the scope file.
- Keep requests, app calculations, and storage code separate inside one app.
- The source `code` identifies a product candidate. Keep its language names together. Check brand, type, and pack before combining records; do not merge different codes by similar names alone.
- Each retailer price belongs to a product and retailer, not directly to one global product price.
- Parse money as decimal amounts. Missing prices stay unknown. Never use zero to stand for missing data.
- Preserve offer text and quantities. Do not treat a multi-buy amount as the price of one pack.
- Keep retrieval time and source date separate. Leave the source date unknown if the file does not provide it.
- Keep the last accepted catalogue when a download or import fails.
- Save only selected products' older prices in the first history pass. No category-average or mixed-pack history.
- A wishlist, shopping list, tracking choice, and actual purchase are different records. Do not make one silently create another.
- All daily jobs run only while the local app is running. First notifications are inside the active app.
- Offline mode first reads a saved shopping list. Disable price refresh and remote chat; label saved amounts and dates.
- Use checked price results in chat. Never invent prices, stock, dates, purchases, or future-price claims.

## 3. Feature references

This table replaces the old difficulty rankings and separate feature legend.

| Earlier ID | Work | Scope reference and boundary |
|---|---|---|
| F1 | Search stored product names, brands, categories, and retailers | R01. Start with language names and simple text search. Spelling tolerance needs its own examples and checks before R01 is complete. |
| F2 | Compare retailer listings | R02; R04 remains conditional. Compare the selected exact product and pack. Show ties and missing listings. |
| F3 | Target-price and price-drop alerts | R21. Two separate rules: a target needs a current price; a drop needs an earlier price for the same product/retailer. |
| F4 | Product-type target alerts | Possible extension of R21. Not a separate confirmed scope row. Agree the category and price rule before implementing it. |
| F5 | Quiet hours, repeat prevention, and snooze | R23. Define event and repeat rules before coding. |
| F6 | Morning summary | R26. Inside the active app first. Set timing and missed-run behaviour before coding. |
| F7 | Saved recurring shopping lists | R27. Product, quantity, and checked state persist across restart. |
| F8 | Per-item lowest listed prices and list total | R28. Use quantities; identify retailers and incomplete totals. This is not R06's full-store basket comparison. |
| F9 | Planned weekly/monthly budget and warnings | R30. Label planned costs; actual spending needs separate purchase entries. |
| F10 | Planned cost by category | R31. Do not label estimates as purchases. |
| F11 | List export and import | R32. A separate copy, with no live sync or accounts. Schedule after the basic list works. |
| F12 | User-set use/interval reminders | R34. Price data does not show household use. |
| F13 | Add a reminder item to a shopping list | R36. Keep product and quantity; compare only saved or successfully refreshed prices. |
| F14 | Clear or snooze a reminder | R37. Define the next due date. |
| F15 | Wishlist | R39. Saving is separate from explicit tracking and from purchases. |
| F16 | View, export, and delete local personal data | R45. List sharing must not export all personal data. |
| F17 | App text and product labels in three languages | R46. The JSON supplies product labels; app screens still need translations. |
| F18 | High-contrast, easy-to-read display | R47. Check text, controls, focus, and meaning without colour. |
| F19 | Read-only saved-list review offline | R48. Data must be on the viewing device; remote price refresh and AI are off. |
| F20 | Selected-product history collection | Supports R10, R14, R21-R22, R35. Use separate product/pack/retailer records; collect only while the app runs. |
| F21 | Price history charts | R10. Show available dates and gaps, not an invented 30/90-day series. |
| F22 | Source and retrieval/source dates | R14 is tracked-only. A general saved-data age label does not imply tracking. |
| F23 | Amount/percentage price-change limits | R22. State the earlier comparison price; a change is not a verified store discount. |
| F24 | Checked price chat and clarification; conditional historical explanation | Required R15/R18. R16/R35 stay conditional; historical comparison is not a future-price prediction. |
| F25 | Voice and five-second answer target | R19 future work. No baseline commitment. |

R03, R05, R07-R09, R12-R13, R17, R33, and R41 remain excluded. R25 and R42 remain future work. Other conditional rows remain in the scope, not silently excluded by this table.

## 4. Jobs we can assign now

These are build jobs, not completed work. Assign one job with its inputs, outputs, and checks. Agree shared field names before two people build connected parts.

| Job | Work and inputs | Required result | Completion check | Depends on |
|---|---|---|---|---|
| J01 | Confirm record fields from the saved JSON sample; add H2 file storage | Products with language labels, retailer listings, import details, and saved list records | Save a product and list, restart the app, and read them again; tests can use a separate memory database | Current Spring Boot setup |
| J02 | Read the sample and then fetch the daily JSON; validate before replacing accepted data | Latest catalogue and original offer text, with retrieval details | Sample imports correctly; invalid/empty responses keep the last good catalogue; a repeated import does not duplicate listings | J01 |
| J03 | Search and exact-product comparison using J02 records | Search results and retailer prices, with ties and missing-state handling | The three source names of one product find the same code; different pack codes stay separate; missing price never wins | J02 |
| J04 | Add list editing and simple screens to J03 | Search -> comparison -> add item -> saved list, including quantities and checked state | Complete the flow in the browser; restart and read the same list; no placeholder prices are presented as source data | J01, J03 |

J04 completes the first milestone. UI work can start as a small sketch while J01-J03 are built, but its fields must follow the agreed records. Spelling tolerance under R01 and the full three-language interface under R46 still need their later checks.

## 5. Following baseline jobs

| Job | Work | Completion check | Depends on |
|---|---|---|---|
| J05 | Wishlist and explicit product tracking, including selected-product history | Saving alone does not start tracking; tracking stores older retailer prices for that exact product; removal stops new collection | J01-J04 |
| J06 | In-app target/drop alerts, quiet hours, snooze, and summary | One current price can meet a target; a drop uses a stated earlier price; repeat imports do not repeat an event; closed-app periods are not reported as delivered alerts | J05 |
| J07 | List totals, planned budgets, and category totals | Quantities affect totals; missing prices make the total incomplete; estimates are not labelled actual spending | J04 |
| J08 | User-set reminders, add-to-list, clear, and snooze | Reminders run only in the active app; actions set the agreed next due date and preserve product/quantity | J04, J06 |
| J09 | Text chat using checked search/comparison operations | The answer uses checked product, retailer, and amount; unclear requests ask for a choice; AI failure still leaves the normal comparison usable | J03 |
| J10 | Separate-copy list sharing | Export and import a chosen list; preserve product/quantity/checked state; invalid input leaves existing lists unchanged | J04 |
| J11 | Read-only saved-list view without a connection | Save on the viewing device, disconnect, and reopen the list; no fresh-price or remote-chat claim; saved amounts show their age | J04 |
| J12 | Complete languages, easy-to-read display, personal-data controls, and history views | Check all three UI languages, useful contrast, data view/export/delete, available history dates, and empty/gap states | Relevant earlier jobs |

J05 can start after product selection works so history begins early. J09 does not need to wait for history. J10's transfer format is chosen when that job starts; it does not block J01-J04.

Before J05, set history retention, re-addition, and recording-date rules. Before J06, set the target boundary, earlier-price rule, first-check alert, repeat reset, summary time, and quiet-hours behaviour. These decisions are listed in the [feasibility review](../feasibility-review-simple.md#5-open-decisions). Agents must not each choose different rules.

Background device/cloud notifications, offline editing, and price prediction are not part of these jobs.

## 6. Shared input and result fields

These are logical fields to agree in J01, not fixed web routes or finished code.

| Operation | Input | Result |
|---|---|---|
| Search | Text and optional language/filter | Product codes and stored labels, with enough pack information to select |
| Compare | Selected product code and retailer filter | Retailer amounts, ties, missing listings, original offers, source/retrieval details |
| Add to list | Product code and quantity | Saved list item or a clear input error |
| Track | Selected product and tracking choices | Saved tracking state; separate from wishlist |
| Explain | Question and checked comparison result | Explanation of those facts or a failure state |

Use these same meanings across screens, app rules, and storage. Do not send the full catalogue to the LLM for each price question. Let app code search and calculate first.

## 7. Checks and current state

Use the existing testing skill and Maven test setup for code changes. A UI picture, a successful compile, or a mock AI answer does not complete a working feature.

The repo currently has a Hello page and one web test. H2, price import, lists, search, history, chat, and alerts are planned additions. Add only the dependencies needed for the assigned job, and record observed test results.

Data inspection was completed separately: see the [JSON sample check](data-sample-review.md). No baseline job above is marked complete by that download.
