# Initial Proposal — Online Price Watch Companion App

## What the data is

A daily-updated dataset from the Consumer Council (Hong Kong) containing **product prices and discount/offer information** across several major local retail brands. This is the core fuel for the app.

---

## 1. Who are the potential users

### Primary personas

| User | What they need | Why they'd use this |
|------|---------------|---------------------|
| **Budget-conscious shoppers** (the biggest group) | Always chase the best price on everyday items | Compare brands in one place instead of hopping between apps/sites |
| **Deal hunters / sale-watchers** | Catch promotions before they end | Real-time offer notifications for the items they care about |
| **Families / household managers** | Plan monthly grocery & household purchases | Bulk-buy timing, spotting when staples are cheapest |
| **Busy professionals** | No time to browse | Ask the agent "where's the cheapest milk today?" and get an instant answer |

### Secondary / niche personas

- **Cross-border shoppers / visitors** — compare Hong Kong prices against what they're used to.
- **Researchers / journalists** (light touch) — track price trends, inflation stories, "price of a basket" articles.
- **Price-conscious seniors** — simple, high-contrast, low-effort deal lookups (accessibility-friendly).

### Who it's NOT for (yet)

- Luxury/big-ticket shoppers (dataset is daily-consumer goods focused).
- People who don't care about saving a few dollars.

---

## 2. What the app could do

### Core functions (MVP-level value)

1. **Price comparison engine** — search any item, see it side-by-side across all brands with the current best price ranked first.
2. **Deal & offer alerts** — follow specific items/brands; push notification the moment a discount or special offer appears.
3. **Agent / advice assistant (the differentiator)** — natural-language ask: *"Which brand has the cheapest rice this week?"* → agent pulls live data and answers with a recommendation + reasoning. This is what separates it from a plain price-listing app.

### Enhancing functions

4. **Price history & trend charts** — is this a good time to buy, or wait? See past prices to judge if a "sale" is actually a bargain.
5. **Shopping list with auto-best-deal** — user builds a list, app suggests the cheapest brand per item and the total.
6. **"Basket price" tracker** — track the cost of a fixed basket of items over time (inflation/grocery-budget view).
7. **Watchlist / favourites** — keep an eye on specific products across brands.
8. **Location-aware suggestions** (future) — recommend where to buy based on the user's nearest branch.
9. **Savings summary** — "You saved $X this month using our recommendations."
10. **Multilingual** — English + Traditional/Simplified Chinese, since the data ships in all three.

---

## 3. Key design considerations to keep in mind (non-technical)

- **Trust is everything.** It's Consumer Council data — the app must make clear it's not affiliated and that prices are as-reported, not real-time guaranteed.
- **Data freshness.** Daily updates are good, but offers can change; the app should say *when* each price was last seen.
- **The agent is the hook.** Price-listing apps exist; the conversational "best place to buy" advice is the differentiator worth building around.

---

## 4. Suggested next steps

1. Confirm the target audience to prioritize (recommended starting point: **budget-conscious shoppers + deal hunters**).
2. Decide whether the MVP is **alert-driven** (push deals) or **agent-driven** (ask questions) first — or both.
3. Once direction is set, we can move into technical architecture.