# Detailed Proposal — Online Price Watch Companion App

*This document expands the initial proposal with concrete requirements gathered from interviews with the four primary target personas. It builds on `initial-proposal.md` and the persona requirement documents in this folder.*

---

## 1. What the data is

A daily-updated dataset from the Consumer Council (Hong Kong) containing **product prices and discount/offer information** across several major local retail brands. It ships in three languages (English, Traditional Chinese, Simplified Chinese) and provides price history, making it a strong foundation for both price comparison and trend analysis. This is the core fuel for the app.

---

## 2. Who the app is for

Four primary personas were interviewed in depth. Each is described below with their defining needs. They share several needs but diverge sharply in *how* they want to interact with the app.

### Persona A — Budget-Conscious Shopper

*Working professional, small HK flat, fixed monthly budget. Wants the cheapest correct thing fast but doesn't trust "cheap" blindly.*

- Compares products across all retailers in one view, keyed on **unit price** (per 100g/L/piece), not just raw price.
- Needs product identity **normalised** (same product may be listed under different names/pack sizes).
- Wants delivery vs in-store pricing, **fees and minimums** surfaced, and full-basket comparison (cheapest single item ≠ cheapest basket).
- Trusts a "best deal" only with **price history, regular price, % saving, discount duration, data source + freshness**, and a *reason* per recommendation.
- Alerting must be **per-item with target prices and thresholds** (HKD 20 off rice, not HKD 1 off a snack), quiet hours, and snooze.
- Budget planning: weekly cycle, actual-spend tracking, "buy now vs wait" advice, bulk-buy guidance that respects **storage and expiry**.
- Retention drivers: one-tap speed, frictionless setup, accuracy over breadth, clear data-source label.

### Persona B — Deal Hunter / Sale-Watcher

*Bargain-shopper, checks daily, hoards discounted staples and electronics. Deeply skeptical of fake discounts.*

- Browsing by category and store brand, fuzzy search in English + Traditional Chinese, a "trending now" feed, and resume-where-I-left-off.
- **Deal honesty is the core need**: original + sale price together, 30/90-day history graphs, "fake discount" warning badges, offer age and expiry date, expired offers marked as such.
- Notification channels split by urgency: push for flash sales, email weekly digest, in-app badge for low priority; deduplicated; quiet hours.
- A dedicated **flash-sale section with countdowns**, pre-expiry alerts (30 min before) on wishlist items.
- Acting on deals: one-tap buy link, save to wishlist, **share to WhatsApp**, copy coupon code, and clear eligibility tags (online-only / in-store-only / member-only).
- Detect the **"bait" pattern** (price raised before a "sale") against the 60-day baseline.

### Persona C — Family / Household Manager

*Full-time parent of two, tracks what the household runs out of, does most of the purchasing.*

- Planning: a weekly list that syncs to an in-store trip, awareness of each item's **purchase cadence** (rice monthly, detergent every ~6 weeks), and bulk-buy flags at the store.
- A "frequent items" list (formula, diapers, toilet paper, oil, rice, eggs, milk) with tracked history, distinguishing **must-have-brand** items from interchangeable generics.
- Budget management: weekly + monthly budgets, running total as the list is built, over-budget warnings with substitute suggestions, category spending breakdown.
- **Shared shopping list** with a partner: who added what, near real-time check-off sync to avoid double-buying, and a "who's shopping" handoff with conflict-safe merging.
- **Basket / inflation tracking** using actual paid prices, not just advertised ones.
- Agent gives **"buy now vs wait"** advice and bulk recommendations that account for storage constraints, with a one-line explanation and honest acknowledgment if a "wait" call was wrong.
- **Re-purchase reminders** based on usage rate, actionable (tap to add to list at best price), snoozable.

### Persona D — Busy Professional ("Kwan")

*39, 60+ hr weeks, shops on the fly between meetings. Wants to spend money but not time thinking about where.*

- **Voice-first, sub-5-second** answers, in mixed Cantonese/English, returning ONE recommendation + one line of reasoning — not a ranked table.
- One-tap "just buy it for me"; recommendations personalised to buying history, brand loyalty, and a "never buy" boundary list.
- A single **morning digest** ("3 things worth knowing today") readable in 10 seconds, plus geo-aware alerts when already near a store.
- Recommendations consider the **commute/trip**, not just absolute price; handle ambiguous queries by defaulting to past purchases.
- Must prove its value: a "money saved" counter and clear data-source timestamps.
- Minimum to keep installed: voice purchase, daily digest, geo-reminder. Anything else is decoration.
- **Privacy is a gate**: wants to see/erase stored history, understand location use, and no data sold to advertisers.

### Secondary / niche personas (unchanged from initial proposal)

- Cross-border shoppers / visitors — compare HK prices to their baseline.
- Researchers / journalists — price trends and inflation stories.
- Price-conscious seniors — simple, high-contrast, low-effort lookups.

### Who it's NOT for (yet)

- Luxury / big-ticket shoppers (dataset is daily-consumer goods focused).
- People who don't care about saving a few dollars.

---

## 3. Core value proposition

The app is not another price-listing site. It does the comparison work the user would otherwise do across several store apps, and — uniquely — it has a **conversational agent** that turns "where should I buy this?" into a direct, reasoned recommendation. For different users it delivers different primary value:

| Persona | Primary value | The one feature they'd keep it for |
|---------|---------------|------------------------------------|
| Budget-conscious shopper | Cheapest correct item, trusted | Price history chart |
| Deal hunter | Catch real deals before they end | Flash-sale + fake-discount detection |
| Household manager | Plan and budget without waste | Shared list + basket tracking |
| Busy professional | A decision made for them | Voice "just buy it" |

---

## 4. Functional requirements

Requirements are grouped by capability, drawing directly on the four persona interviews. Effort is indicated as **Core** (essential to the value proposition / MVP), **Enhancing** (strong added value), or **Future** (nice-to-have / later).

### 4.1 Product search & comparison — *Core*

- Search by product, category, and store brand; fuzzy matching that tolerates misspellings and mixed English + Traditional Chinese.
- Side-by-side comparison of the same product across all participating retailers in one view.
- **Unit price** (per 100g / per L / per piece) as the primary sort key.
- **Product normalisation** — group the same product even when listed under different names or pack sizes across stores.
- Distinct delivery vs in-store pickup pricing, with **fees, minimums, and estimated delivery time** surfaced.
- Full-basket comparison (cheapest single item may not be at the cheapest-basket store).
- Flag out-of-stock / unavailable items clearly; keep history but mark discontinued products.
- Enrichment signals (quality/review indicators) so cheapest isn't assumed best.

### 4.2 Deal honesty & trust — *Core*

- Show original (pre-discount) price and sale price together, and the actual % saving.
- **Price history graphs** (last 30/90 days) per item.
- **Fake-discount detection** — warn when a sale price equals or exceeds the recent average / 60-day baseline (the "bait" pattern).
- Show how long an offer has been running and when it expires; mark expired offers as expired.
- Clearly display the **data source and last-updated timestamp** ("Consumer Council, updated <date>") so freshness is never in doubt.
- Provide a one-line reason per recommendation ("cheapest unit price", "lowest delivered total", "cheapest in 3 months").

### 4.3 The agent (conversational advice) — *Core / the differentiator*

- Natural-language ask in English / Chinese / mixed: *"Where's the cheapest milk today?"*
- Returns **ONE recommendation + store + price + a one-line reason**, not a ranked table.
- **Buy now vs wait** advice based on whether the current price is a genuine low or normal fluctuation.
- **Bulk advice** that respects storage constraints (stock up on rice/detergent/canned goods, not fresh food) and warns against hoarding.
- Handles ambiguity ("milk") by asking one clarifying question or defaulting to the user's past purchase.
- Voice input and sub-5-second response (critical for the busy professional).
- Honest acknowledgment when a "wait" recommendation was wrong.

### 4.4 Alerts & notifications — *Core*

- Per-item **target-price price-drop alerts** (alert only below the user's threshold), not blanket deal noise.
- Threshold controls (absolute saving, e.g. HKD 20 off rice, or % saved) and **quiet hours**.
- Multiple channels by urgency: push for flash sales, email weekly digest, in-app badge for low priority.
- Deduplication — never repeat the same price level already notified.
- Geo-aware alert when the user is already near a store with a wanted item on offer (requires a clearly-explained location toggle).
- **Snooze** an item (stocked up — stop nagging for 3 months).
- Optional single **morning digest** summarising the day's best opportunities in one 10-second read.

### 4.5 Shopping list, basket & budget — *Enhancing*

- Build and save recurring shopping lists; re-check in one tap.
- **Auto-best-deal** per list item and a basket total.
- **Basket price tracker** over time to gauge household inflation, using **actual paid prices** (not just advertised).
- Weekly and monthly budgets, a running estimated total as the list grows, and over-budget warnings with substitute/drop suggestions.
- Category spending breakdown (fresh food, baby, cleaning, toiletries).
- **Shared list** with a partner: attribution, near real-time check-off sync, and a "who's shopping" handoff with conflict-safe merging (or locking).
- Bulk-buy flags when a larger pack is cheaper per unit, accounting for storage and expiry.

### 4.6 Re-purchase reminders — *Enhancing*

- Reminders based on **usage rate** rather than a fixed schedule.
- Recommend buying now (if there's a deal) or waiting (if prices are expected to hold).
- Actionable — tapping a reminder adds the item to the list at the best current price.
- Easily acknowledged or snoozed.

### 4.7 Acting on a deal — *Enhancing*

- One-tap link to buy at the store.
- Save to wishlist to track price and get alerts.
- **Share to WhatsApp** and copy coupon codes with one tap.
- Show eligibility tags (online-only / in-store-only / member-only) and coupon stackability before a trip.
- Buy directly from the app or get a walking route + estimated time to the store.

### 4.8 Savings & value transparency — *Enhancing*

- "Money saved this month" counter.
- Cost change vs last week/month.
- A "what you've told me" privacy audit screen, with one-tap export/delete of stored data.

### 4.9 Accessibility & language — *Future*

- English + Traditional/Simplified Chinese.
- Simple, high-contrast, low-effort mode for price-conscious seniors.
- Offline/cached usability for in-store shopping on limited data.

---

## 5. Cross-cutting design principles

These emerged repeatedly across all four interviews and should govern product decisions.

1. **Relevance beats frequency.** Alert fatigue is the single biggest reason users would mute and abandon the app. Every notification must pass a personal threshold (item, target price, quiet hours, dedup).
2. **Trust through price history.** Almost every persona distrusts "sales" without a baseline. Price history and source/freshness transparency are non-negotiable — they're the trust engine that makes every other recommendation credible.
3. **"Cheapest" is not "best."** A recommendation must account for delivery fees, stock, unit size, brand constraints, storage/expiry, commute, and personal history. A raw price ranking will feel like a lie and break trust.
4. **Normalisation is foundational.** Duplicate/mismatched product listings are the biggest trust killer. Getting identity normalisation right underpins comparison, history, and alerts alike.
5. **Different users, different speed.** The budget shopper wants a fast, accurate comparison; the busy professional wants a decision made for them; the deal hunter wants urgency; the household manager wants planning. The app must let each persona use it the way they prefer rather than forcing one interaction model.
6. **Personalisation with explicit privacy.** Users accept personalisation only if it's local, auditable, and erasable, with location use clearly explained. Trust is the reason they'd keep it installed; selling data would kill it. A small subscription to avoid ads/data-sale is acceptable to the most privacy-sensitive persona.
7. **Data honesty.** Clearly label Consumer Council as the source, state the app is not affiliated, and always show the last-updated timestamp. Say "not enough history yet" rather than give shaky advice.

---

## 6. Suggested MVP cut

The MVP should deliver the differentiator and the trust engine, while keeping scope tight.

**In scope for MVP (Core):**
- Product search + side-by-side comparison with unit pricing and normalisation.
- Price history graphs and fake-discount detection (the trust engine).
- The agent (text first), returning a single reasoned recommendation with "buy now vs wait" and bulk advice.
- Per-item target-price alerts with quiet hours and dedup.
- Basic watchlist / favourites.

**Deferred (Enhancing / Future):**
- Shared lists, budgets, basket inflation tracking, re-purchase reminders (household manager depth).
- Voice input, geo-alerts, morning digest, buy-direct (busy-professional depth — voice is the highest-value, highest-effort item).
- Coupon codes, eligibility tags, WhatsApp share, flash-sale countdowns (deal-hunter depth).
- Offline mode, multilingual polish, senior accessibility mode.

> Note on voice and geo-alerts: these are the busy professional's top features but are also the most complex and privacy-sensitive. Recommend building them **after** the text agent proves value, and treating the morning digest (cheap, high-impact) as the first step toward that persona.

---

## 7. Persona prioritisation and next steps

**Recommended first target: budget-conscious shopper + deal hunter.** These two share the strongest trust-and-comparison needs and are the easiest to satisfy with the dataset. The agent adds the differentiating value on top.

1. Validate the MVP feature cut with a small user group.
2. Confirm which of the deferred "enhancing" features move into the first release (household manager's shared list is a strong early candidate for retention).
3. Decide the interaction model for the first release — text agent + alerts (recommended) before voice.
4. Only then move into technical architecture, data modelling (especially product normalisation and price history), and notification design.