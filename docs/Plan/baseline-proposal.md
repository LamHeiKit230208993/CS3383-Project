# Baseline Proposal — Online Price Watch Companion App

*This document defines the **confirmed baseline scope** for the app, derived from `feasibility-analysis.md` and `detailed-proposal.md`. It separates what we will actually build (baseline), what is baseline but conditioned on a local history feature, what is deferred/low-priority, and what is out of scope for this project. Feasibility is assessed against the Consumer Council Online Price Watch dataset as the sole data source.*

---

## 1. Requirement classification legend

Every requirement below is tagged with one of four markers:

| Marker | Meaning |
|--------|---------|
| `+` | **Baseline** — confirmed, in current scope. |
| `!` | **Baseline, conditioned** — confirmed, but only delivers value once the **local history-tracking** feature is active (see §5). |
| `#` | **Future** — not baseline; planned for later, may or may not be implemented. |
| `~` | **Baseline but deferred** — low priority; implement later. |

Requirements with no marker in `feasibility-analysis.md` are **out of scope for this project** and are not listed here.

---

## 2. What the data actually provides (unchanged)

The dataset (English CSV, daily snapshot) has exactly **9 columns**:

| Column | Content |
|--------|---------|
| `Category 1` / `Category 2` / `Category 3` | Hierarchical product category |
| `Product Code` | Unique product identifier (e.g. `P000000192`) |
| `Brand` | Product brand (e.g. Garden, Quaker) |
| `Product Name` | Product name — **size/weight embedded in free text** |
| `Supermarket Code` | Retailer — **9 distinct**: AEON, DCHFOOD, JASONS, LUNGFUNG, MANNINGS, PARKNSHOP, SASA, WATSONS, WELLCOME |
| `Price` | A single numeric price (appears to be current price) |
| `Offers` | **Free-text** offer string (e.g. "Buy 2 to save $4.00", "2 for $14.00", "20%off") |

Key structural facts shaping scope:

- **Product identity is partially solved** — `Product Code` is shared across retailers, so the same product at different stores can be grouped directly.
- **No structured size/quantity field** — unit price cannot be computed reliably.
- **No reference/regular price** — % discount cannot be directly verified.
- **No delivery/stock/location/date/eligibility data.**
- **No live price history** — history requires accumulating our own snapshots over time.
- **Data is a daily snapshot** — not real-time; no flash-sale countdowns.

---

## 3. Baseline requirements (`+`)

These are confirmed and deliverable with the data as-is.

### 3.1 Product search & comparison

| Req | Marker | Note |
|-----|--------|------|
| Search by product, category, store brand; fuzzy matching EN+Chinese | `+` | Directly searchable on Product Name/Brand/Category. Multilingual data exists. |
| Side-by-side same product across all retailers | `+` | Group by `Product Code`. |

### 3.2 The agent (conversational advice)

| Req | Marker | Note |
|-----|--------|------|
| Natural-language ask, one recommendation + reason | `+` | Answers "cheapest at which store" from `Price`. Reasoning limited to raw price (honest caveat). |
| Handle ambiguity / default to past purchases | `+` | Answering itself is feasible; personalisation uses user data we store. |

### 3.3 Alerts & notifications

| Req | Marker | Note |
|-----|--------|------|
| Per-item target-price price-drop alerts | `+` | Alerts when a watched item's price changes between our own daily snapshots. Not real-time; works once history is stored. |
| **Favourite a product type and alert on target price** (e.g. add "milk" to favourites; notify when any milk is at or around $5) | `+` | Category-level watch: user favourites a **type of goods**, sets a target price, and is notified when any item of that type reaches that price. Supported by `Price` + `Category` fields directly — no history needed. |
| Quiet hours, dedup, snooze | `+` | Pure app-side settings. |
| Morning digest | `+` | Summarise from the daily snapshot. |

### 3.4 Shopping list, basket & budget

| Req | Marker | Note |
|-----|--------|------|
| Save recurring lists, re-check in one tap | `+` | App-side. |
| Auto-best-deal per item + basket total | `+` | Feasible as raw-price best; not unit-price or delivery adjusted. |
| Weekly/monthly budget, running total, warnings | `+` | App-side; price data supports estimated totals. |
| Category spending breakdown | `+` | Categories exist in data. |
| Shared list, sync, handoff | `+` | App-side. |

### 3.5 Re-purchase reminders

| Req | Marker | Note |
|-----|--------|------|
| Reminders based on usage rate | `+` | App-side (user-set). |
| Actionable — tap to add at best price | `+` | App-side. |
| Acknowledge / snooze | `+` | App-side. |

### 3.6 Acting on a deal

| Req | Marker | Note |
|-----|--------|------|
| Save to wishlist | `+` | App-side. |

### 3.7 Savings & value transparency

| Req | Marker | Note |
|-----|--------|------|
| Privacy audit screen / export/delete | `+` | App-side. |

### 3.8 Accessibility & language

| Req | Marker | Note |
|-----|--------|------|
| EN + Traditional/Simplified Chinese | `+` | Three language resources exist. |
| Senior high-contrast mode | `+` | App-side UI. |
| Offline/cached usability | `+` | App-side (cached snapshots). |

---

## 4. Baseline, conditioned on local history-tracking (`!`)

These are baseline requirements, but they only deliver real value once the local history-tracking feature (§5) has accumulated data. They are in scope — not deferred — but their usefulness is gated by the history mechanism.

| Req | Marker | Note |
|-----|--------|------|
| Price history graphs (30/90-day) | `!` | Not provided live. Achievable after the app accumulates its own snapshots for the tracked product types; not available on day one. |
| Data source + last-updated timestamp | `!` | We can display the snapshot date ourselves. |
| Threshold (% / absolute) | `!` | Absolute delta computable; % delta needs a reference/baseline — we use a previous snapshot of the tracked category. |
| Buy now vs wait advice (re-purchase reminders) | `!` | "Buy now" (cheapest store) feasible; "wait" needs a trend from the tracked history. |

---

## 5. The local history-tracking mechanism (baseline, `!`)

This is the **first-class baseline requirement** that the four `!` items depend on. It is a deliberate feature, not an accident of the data.

**Definition:** The user selects a **product type** (a category such as "milk", "rice", "detergent"). The app then **downloads and stores that category's daily snapshots locally for a set period** (e.g. 30/90 days), enabling comparison and trend analysis for that type.

**Why it's needed:** the dataset ships no live price history. History, fake-discount detection, buy-now-vs-wait, and %-based thresholds all require a baseline. Tracking **user-selected types** (rather than everything) keeps storage and download scope bounded and focuses history where the user cares.

**Behaviour:**
- User picks a product type (or a specific item).
- App begins storing that type's daily snapshots locally for a chosen period.
- Once enough snapshots accumulate, the `!` features unlock for that type.
- This is a **baseline requirement** — the architecture must support it from day one, even if full history-based value arrives later.

---

## 6. Future requirements (`#`)

Planned for later; may or may not be implemented. Not baseline.

| Req | Marker | Note |
|-----|--------|------|
| Geo-aware alerts (near a store) | `#` | No branch/location data. Would need a geolocation feature + additional data. |
| Buy directly / walking route | `#` | Route needs geo data; buy-direct needs a link/checkout. |

---

## 7. Baseline but deferred / low priority (`~`)

| Req | Marker | Note |
|-----|--------|------|
| Voice input, sub-5s | `~` | Voice is a UI/tech concern, not data-limited. Baseline but implement later; low priority. |

---

## 8. Out of scope for this project

The following requirements from `detailed-proposal.md` are **not** part of the baseline because the data cannot support them, and they are not being pursued:

- Unit-price / unit-pricing comparison (no structured size field).
- Product normalisation across codes (inconsistent sizes).
- Delivery vs in-store pricing, fees, minimums.
- Full-basket comparison (no delivery/unit-price normalisation).
- Out-of-stock / discontinued flags (no stock field).
- Enrichment / quality signals (no reviews/ratings).
- Original + sale price + % saving (no reference price).
- Fake-discount detection (needs a baseline we don't track for all items).
- Offer age / expiry date, expired-offer marking (no date fields).
- Bulk advice (storage-aware) — no size/quantity data.
- Honest acknowledgment when a "wait" call was wrong (needs history we don't track broadly).
- Multiple channels by urgency / flash-sale urgency (no real-time data).
- Basket inflation tracking (needs broad history).
- Bulk-buy flags (bigger pack cheaper per unit) — needs unit size.
- One-tap link to buy (no product URL).
- Share to WhatsApp / copy coupon code (no coupon codes in data).
- Eligibility tags (online/in-store/member-only) — no flags.
- "Money saved this month" counter (needs a baseline).
- Cost change vs last week/month (needs broad history).

---

## 9. Bottom line

The dataset is a **flat daily price snapshot**, not a rich e-commerce feed. Its strengths are a reliable `Product Code` for cross-store grouping and free-text offer descriptions. Its weaknesses are the absence of unit size, reference price, delivery/stock/location/date/eligibility data, and no real-time or live history.

**Baseline delivers:**
- **Raw-price comparison** + "where's cheapest" agent (presented honestly as lowest listed price).
- **Watchlist / alerts / digests** on the daily snapshot (per-item watch, **product-type target-price alerts**, absolute price-drop, quiet hours, dedup, snooze).
- **Shopping list, basket raw-total, budget, category breakdown, shared list, reminders.**
- **Multilingual + senior mode + offline cache + privacy controls.**
- **The local history-tracking mechanism** (`!`), so the trust features (history graphs, % thresholds, buy-now-vs-wait) unlock over time for user-selected product types.

**Deferred:** voice input (`~`), geo-alerts & buy-direct/walking-route (`#`).

**Explicitly out of scope:** every requirement the data cannot support — unit price, delivery, stock, reference price, dates, eligibility, coupon codes, fake-discount, and the features that need broad history.

The honest framing is central: the app delivers **lowest listed price** and says so, builds its **own local history for user-picked types**, and defers rather than half-implements anything the data cannot truthfully support.