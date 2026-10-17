# Product Description

Status: Current description, aligned with the [project scope](project-scope.md). Feature decisions are maintained there.

## Purpose

Help Hong Kong shoppers compare listed prices for daily goods, track selected items, and plan household shopping.

The app has one local user, with no account, sign-in, or cloud saving. A user can share a shopping list as a link or file. The recipient imports it as a separate local list; later edits do not sync.

Use Spring Boot and save local records across restarts. First reminders and alerts appear inside the active app. Offline mode first allows read-only review of a saved shopping list; it does not fetch prices or call the remote AI.

The course focuses on using AI in software engineering. The app's price chat is one product feature. This document defines the product, not the development workflow.

## Users

The intended users include shoppers managing a budget, people following daily deals, and household shoppers. Busy workers can also use the supported features, but voice buying and location alerts are future ideas.

The four [user-needs files](#user-needs) contain requests generated through AI role-play. They are input to the team’s requirements review. Requests describe what a user wants; the project scope records what the team will build.

## Shared terms

| Term | Meaning |
|---|---|
| Product | A named item with its brand, type, and pack details. |
| Listing | A retailer's recorded price and offer text for a product. |
| Retailer | A store company. It does not identify an individual branch. |
| Listed price | The price shown in a source record; delivery cost and stock may be unknown. |
| Tracked item | An item the user explicitly adds for tracking. Features marked ! apply only to these items. |
| Wishlist item | An item the user saves. Saving alone does not start tracking or record a purchase. |
| Shopping-list item | A product and quantity the user plans to buy. It is not proof of a purchase. |
| Shared list copy | An exported list that another person can import. Each copy is edited separately. |
| Purchase entry | A record of an actual purchase, including quantity and amount paid. The entry method is still open. |
| Price history | Dated price records. The chart can only cover the dates available. |
| Latest catalogue | The most recently accepted product and retailer price records, used for search. This is separate from selected products' older history. |
| Target-price alert | A message when a current price meets the user's chosen limit. |
| Price-drop alert | A message when a current price falls relative to a stated earlier price. |

## Main user actions

| User action | Expected result | Scope references |
|---|---|---|
| Search and select a product | Matching products, then comparable retailer listings | R01–R04 |
| Ask a price question | A checked price comparison and a short explanation | R15, R18 |
| Add an item for tracking | Supported history and alert functions for that item | R10, R14, R21–R23 |
| Plan shopping | Saved products, quantities, estimated totals, and budget checks | R27–R32 |
| Manage reminders | Clear, snooze, or add an item to a list | R34–R37 |
| Manage saved data and display | Personal-data controls, languages, contrast, and saved offline data | R39, R45–R48 |

These are related feature ranges, not a list of accepted features. They contain excluded and conditional rows; read each scope decision before assuming support.

## Product limits

Use the latest available daily data. Do not describe it as live store stock or a guaranteed checkout price. Compare exact products and packs. Historical price differences do not establish a future price or a fake discount.

For example, if one store lists the selected pack at HKD 40 and another at HKD 45, the app can report a HKD 5 listed-price difference. This made-up example does not include delivery, stock, or purchase confirmation.

## User needs

- [Budget-conscious shopper](user-needs/budget-conscious-shopper.md)
- [Deal hunter](user-needs/deal-hunter.md)
- [Household shopper](user-needs/household-shopper.md)
- [Busy worker](user-needs/busy-worker.md)
