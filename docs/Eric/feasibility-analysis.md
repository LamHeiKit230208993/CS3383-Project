# Feasibility Analysis — Online Price Watch Companion App

*Feasibility assessed against the **actual data schema** of the Consumer Council Online Price Watch dataset, treated as the **sole data source**. This document evaluates each requirement from `detailed-proposal.md` against what the data can and cannot support, and flags the requirements that remain **unfulfilled** under that constraint.*

---

## 1. What the data actually provides

The dataset (English CSV, daily snapshot) has exactly **9 columns**:

| Column | Content |
|--------|---------|
| `Category 1` / `Category 2` / `Category 3` | Hierarchical product category |
| `Product Code` | Unique product identifier (e.g. `P000000192`) |
| `Brand` | Product brand (e.g. Garden, Quaker) |
| `Product Name` | Product name — **size/weight embedded in free text** (e.g. "300g", "8 slices") |
| `Supermarket Code` | Retailer — **9 distinct**: AEON, DCHFOOD, JASONS, LUNGFUNG, MANNINGS, PARKNSHOP, SASA, WATSONS, WELLCOME |
| `Price` | A single numeric price (appears to be current price) |
| `Offers` | **Free-text** offer string (e.g. "Buy 2 to save $4.00", "2 for $14.00", "20%off", "Buy 1 get 1 free") |

### Key structural facts that shape feasibility

- **Product identity is partially solved.** The `Product Code` is shared across retailers, so the same product at different stores can be grouped directly — this is a real advantage.
- **No structured size/quantity field.** Sizes are only embedded in free-text `Product Name`. **Unit price cannot be computed reliably** without parsing text and is fragile/inconsistent across stores.
- **No reference/regular price.** `Price` is the only price. **There is no "was $" value, so % discount cannot be directly verified.**
- **Discount % is only embedded in `Offers` free text** (some contain "20%off", some only "Buy 2 to save $4.00"). Not structured; no guarantee of consistency.
- **No delivery fees, no minimums, no stock/availability, no expiry/offer end date, no branch/location, no member/online-only flags.**
- **No live price history** in the daily file. Historical archives exist via download, but are **not a live API field** — history requires accumulating our own snapshots over time.
- **Data is a daily snapshot** — not real-time. "Flash sale countdowns" and sub-daily urgency are not supported.

---

## 2. Feasibility verdict by requirement

Legend: ✅ **Feasible** with the data as-is · ⚠️ **Partially feasible** (needs derivation, workarounds, or caveats) · ❌ **Not feasible** with this data alone.

### 4.1 Product search & comparison

| Req | Verdict | Note |
|-----|---------|------|
| Search by product, category, store brand; fuzzy matching EN+Chinese | ✅ + | Directly searchable on Product Name/Brand/Category. Multilingual data exists (EN/zh-Hant/zh-Hans resources). |
| Side-by-side same product across all retailers | ✅ + | Group by `Product Code`. |
| **Unit price** as primary sort | ❌ | No size field. Sizes are in free text and inconsistent; unit price is not reliably computable. |
| **Product normalisation** (same product, diff names/sizes) | ⚠️ | `Product Code` groups most duplicates, but a single code can still appear with variant pack sizes; cross-code dedup of "same" product is not guaranteed. |
| Delivery vs in-store pricing, fees, minimums | ❌ | No delivery/fee/minimum data at all. |
| Full-basket comparison | ⚠️ | Sum of prices is computable, but no delivery/fees and no unit-price normalisation, so "cheapest basket" is only a raw price sum. |
| Flag out-of-stock / discontinued | ❌ | No stock field. Absence from the snapshot can be inferred but is unreliable (may just be unlisted). |
| Enrichment/quality signals | ❌ | No reviews/ratings in the data. |

### 4.2 Deal honesty & trust 
| Req | Verdict | Note |
|-----|---------|------|
| Show original + sale price + % saving | ❌ | No reference/regular price. % saving not verifiable. |
| **Price history graphs** (30/90-day) | ⚠ !️ | Not provided live. Achievable **only after accumulating our own daily snapshots** over time; not available on day one. |
| **Fake-discount detection** | ⚠️ | Only once we build our own history baseline; impossible from a single snapshot. |
| Offer age / expiry date | ❌ | No offer start/end date. |
| Mark expired offers | ❌ | No expiry data. |
| Data source + last-updated timestamp | ✅ ! | We can display the snapshot date ourselves. |

### 4.3 The agent (conversational advice)

| Req | Verdict | Note |
|-----|---------|------|
| Natural-language ask, one recommendation + reason | ⚠ + ️ | Can answer "cheapest at which store" from `Price`. **But** a genuine "best" requires unit price, delivery, stock, and history — so the reasoning is limited to raw price. |
| Buy now vs wait | ⚠️ | Needs history (own snapshots). "Wait" is unactionable without a trend. |
| Bulk advice (storage-aware) | ❌ | No size/quantity data and no user-storage inputs in data; advice is speculative. |
| Handle ambiguity / default to past purchases | ⚠ + | Answering itself is feasible; personalisation requires user data we store, not the API. |
| Voice input, sub-5s | ⚠ ex️ | Voice is a UI/tech concern, not data-limited; but good answers need the missing data above. |
| Honest acknowledgment when a "wait" call was wrong | ⚠️ | Only meaningful with history; requires our own tracking. |

### 4.4 Alerts & notifications

| Req | Verdict | Note |
|-----|---------|------|
| Per-item target-price price-drop alerts | ⚠ +️ | Can alert when a watched item's price changes **between our own daily snapshots**. Not "real-time"; works after we start storing history. |
| Threshold (% / absolute) | ⚠ !️ | Absolute delta is computable; **% delta needs a reference price** which we lack (only can use a previous snapshot as baseline). |
| Quiet hours, dedup, snooze | ✅ + | Pure app-side settings. |
| Multiple channels by urgency | ⚠️ | Feasible, but "flash sale" urgency is impossible (no real-time data). |
| Geo-aware alerts (near a store) | ❌ # | No branch/location data. |
| Morning digest | ✅ + | Summarise from the daily snapshot. |

### 4.5 Shopping list, basket & budget

| Req | Verdict | Note |
|-----|---------|------|
| Save recurring lists, re-check in one tap | ✅ + | App-side. |
| Auto-best-deal per item + basket total | ⚠ +️ | Feasible as raw-price best; **not** unit-price-adjusted or delivery-adjusted. |
| **Basket inflation tracking** over time | ⚠️ | Feasible once we accumulate own snapshots. |
| Weekly/monthly budget, running total, warnings | ✅ + | App-side; price data supports estimated totals. |
| Category spending breakdown | ✅ + | Categories exist in data. |
| Shared list, sync, handoff | ✅ + | App-side. |
| Bulk-buy flags (bigger pack cheaper per unit) | ❌ | Needs unit size; not computable. |

### 4.6 Re-purchase reminders

| Req | Verdict | Note |
|-----|---------|------|
| Reminders based on usage rate | ✅ + | App-side (user-set). |
| Recommend buy now vs wait | ⚠ !️ | "Buy now" (cheapest store) feasible; "wait" needs history. |
| Actionable — tap to add at best price | ✅ + | App-side. |
| Acknowledge / snooze | ✅ + | App-side. |

### 4.7 Acting on a deal

| Req | Verdict | Note |
|-----|---------|------|
| One-tap link to buy | ⚠️ | We know the retailer but have **no product/URL to link to**; a store link is possible, item-level is not. |
| Save to wishlist | ✅ + | App-side. |
| Share to WhatsApp, copy coupon code | ⚠️ | Share is fine; **coupon codes are not in the data** (only offer text). |
| Eligibility tags (online/in-store/member-only) | ❌ | No such flags. |
| Buy directly / walking route | ⚠ #️ | Route needs geo data we don't have; buy-direct needs a link/checkout which we don't have. |

### 4.8 Savings & value transparency

| Req | Verdict | Note |
|-----|---------|------|
| "Money saved this month" | ⚠️ | Requires a reference price (baseline) to measure savings; only computable vs our own history. |
| Cost change vs last week/month | ⚠️ | Feasible once we keep our own snapshots. |
| Privacy audit screen / export/delete | ✅ + | App-side. |

### 4.9 Accessibility & language

| Req | Verdict | Note |
|-----|---------|------|
| EN + Traditional/Simplified Chinese | ✅ + | Three language resources exist. |
| Senior high-contrast mode | ✅ + | App-side UI. |
| Offline/cached usability | ✅ + | App-side (cached snapshots). |

---

## 3. Requirements NOT fulfillable with this data alone

These requirements **cannot be met** using the API as the only source, and would need either (a) additional data sources, (b) the user to supply the info, or (c) to be dropped:

1. **Unit price / unit-pricing comparison** — no structured size field; sizes are inconsistent free text.
2. **Delivery vs in-store pricing, delivery fees, delivery minimums** — no such data.
3. **Stock / availability / out-of-stock flags** — no stock field.
4. **Regular/reference price and true % discount** — only a single price is provided; no "was" value.
5. **Offer start/end dates, offer age, expired-offer marking** — no date fields.
6. **Member-only / online-only / in-store-only eligibility tags** — no flags.
7. **Coupon codes and stackability** — not in the data (only free-text offer descriptions).
8. **Store branch / location / geo-aware alerts / walking routes** — no location data.
9. **Buy-direct / item-level purchase links** — no product URLs or checkout integration.
10. **Bulk-buy / per-unit-size cost advice (storage-aware)** — no unit size.
11. **Real-time / flash-sale countdown urgency** — data is a daily snapshot.
12. **Quality / review signals** — not in the dataset.

## 4. Requirements that are deferred (not impossible, but not available on day one)

These **become** feasible only after the app has been running and storing its own daily snapshots:

- **Price history graphs** (30/90-day).
- **Fake-discount detection** (bait-pattern) — needs a historical baseline.
- **Buy now vs wait** advice — needs a trend.
- **Basket inflation tracking** — needs accumulated snapshots.
- **"Money saved" counter** — needs a baseline reference price.
- **Price-drop alerts** — work only once the app has a previous snapshot to compare against (alerts are snapshot-to-snapshot, not real-time).

> Note: historical data archives are downloadable, which could bootstrap price history faster, but they are **bulk download files, not a live API field** — treating the API as the only source means the app must accumulate its own history (or ingest the archives).

## 5. Requirements fully feasible with the data

These are solid and can be delivered with confidence:

- Search and side-by-side comparison by `Product Code` across retailers.
- Category browsing and brand browsing.
- Multilingual (EN / zh-Hant / zh-Hans).
- **Raw-price** "cheapest at which store" agent answers (the core ask), with one-line reasoning.
- Alerts and digests based on the daily snapshot (per-item watch, absolute price-drop, quiet hours, dedup, snooze).
- Shopping list, basket raw-total, budget tracking, category breakdown, shared list, reminders.
- Data-source and last-updated transparency.
- App-side features: privacy controls, senior mode, offline cache.

---

## 6. Bottom line

The dataset is a **flat daily price snapshot**, not a rich e-commerce feed. Its strengths are a **reliable `Product Code` for cross-store grouping** and **free-text offer descriptions**. Its weaknesses are the **absence of any unit size, reference price, delivery/stock/location/date/eligibility data**, and **no real-time or live history**.

**What this means for the product:**

- The **core comparison + "where's cheapest" agent** is achievable but must be presented as a **raw-price comparison**, not a unit-price or total-cost-of-ownership comparison. Without unit price, delivery fees, and stock, the honest answer is "lowest listed price," and the app must say so to preserve trust.
- **Trust features (history, fake-discount, buy-now-vs-wait) are all delayed** until the app accumulates its own snapshots — so the "trust engine" is a **day-1 architecture requirement, not a day-1 deliverable.**
- The **busy-professional and deal-hunter differentiators** (voice buy-direct, geo-alerts, coupon codes, eligibility tags, real-time flash sales, unit-price-aware best-deal) are **largely blocked** by the data and would need additional sources or user-supplied context.

**Recommended focus under the single-source constraint:** a trustworthy **raw-price comparison + per-item watchlist/alert + basic agent** built on `Product Code` grouping and the daily snapshot, with **price-history accumulation designed in from day one** so the trust features unlock over time. The features that require richer data should be **explicitly deferred** rather than half-implemented, to avoid promising comparisons the data cannot deliver.