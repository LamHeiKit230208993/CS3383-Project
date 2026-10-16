# User Needs: Deal Hunter

[Request source and shared terms](../product-description.md) · [Team decisions](../project-scope.md)

## Situation

The shopper checks offers during lunch or in the evening. They save products for later and want to distinguish a useful price change from an attention-grabbing sale label.

## Requests

- “Let me browse by product category and retailer, and find an item even if I type its name differently.”
- “Keep my saved items easy to find when I return.”
- “Show the current price beside earlier recorded prices so I can judge the offer.”
- “Let me choose a target for each item and control when messages arrive.”
- “Send a useful summary instead of many repeated messages.”
- “Let me share a result and open a purchase page.”
- “If a deal needs a coupon or membership, explain that before I act.”
- “Tell me when an offer ends. I would also like alerts for short sales.”

The original requests also mentioned electronics and popular deals watched by other users. Product coverage and a popularity feed are not established requirements; they must not be inferred from category browsing.

## Problems to handle

- A “sale” price is higher than a previous recorded price. Show the comparison without claiming that it proves deception.
- A price meets the chosen target even though it did not drop since the previous update. These are different alert conditions.
- A listing disappears. This does not prove that an offer expired or the product is out of stock.
- A shared result is opened later. Its recorded date matters.
- A daily file cannot support a claim that a short sale has just started or is about to end.

## Scope links

| Request group | Team decision |
|---|---|
| Search, comparison, alerts, quiet hours, summary, wishlist | Build with stated limits: R01–R02, R21–R23, R26, R39 |
| History and checking advertised discounts | Conditional: R10–R11; R10 is tracked-only |
| Purchase-page links, sharing, coupon copying | Conditional: R38, R40; sharing and coupon copying are separate actions |
| Regular-price discount claims, expiry labels, eligibility labels | Excluded: R09, R12–R13, R41 |
| Buying within the app | Future: R42 |

The user's wish for immediate deal information remains useful context. It does not turn daily data into a live feed.
