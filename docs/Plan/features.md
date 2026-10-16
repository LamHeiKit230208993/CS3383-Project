# Baseline Features — Spring Boot Implementation Plan

*This document defines each baseline feature from `baseline-proposal.md` as a concrete, implementable requirement for a **Spring Boot web app**, then orders them by implementation difficulty and business importance into a recommended build sequence. Assumptions are fixed for the baseline: single user, no auth, live API + daily snapshots, H2 database, real LLM integration.*

---

## 0. Build assumptions

| Decision | Choice | Note |
|----------|--------|------|
| Framework | Spring Boot | Per `architecture-options.md` |
| Database | **H2** (in-memory/file) | Zero-setup for the baseline; Postgres is a later swap |
| Auth | **Single user, no auth** | Watchlists/lists/budgets are global |
| Data source | **Live API + daily snapshot** | Fetch `pricewatch_en.csv` on a schedule, store to DB |
| Agent | **Real LLM integration** | Actual API call, not a stub |
| Privacy export | **Export & download as Markdown** | Summary/list; plus delete local data |
| Language | EN + zh-Hant + zh-Hans | Multilingual |

Out-of-scope requirements (unit price, delivery, stock, reference price, dates, eligibility, coupon codes, fake-discount, broad-history features) are **not** built; they are listed once in §6.

---

## 1. Feature inventory

Each feature: what it is, data it uses, Spring concern, marker, and difficulty.

### Search & comparison (`+`)

**F1 — Fuzzy product search (EN + Chinese)**
- **What:** Search products by name, brand, or category; tolerate misspellings and mixed EN/Traditional/Simplified Chinese.
- **Data:** `Product` (name, brand, category). Searching the multilingual resources.
- **Spring:** `ProductController` + `ProductRepository` (`findByNameContaining`, `findByBrand`, `findByCategory`).
- **Marker:** `+` · **Difficulty:** Low.

**F2 — Side-by-side comparison across retailers**
- **What:** Show the same product at all 9 retailers in one view, grouped by `Product Code`.
- **Data:** `Product` grouped by `Product Code`.
- **Spring:** `ProductRepository` (`findByProductCode`), `ComparisonController` / `ComparisonService`.
- **Marker:** `+` · **Difficulty:** Low.

### Alerts & notifications (`+`)

**F3 — Per-item target-price drop alerts**
- **What:** When a watched item's price drops (between daily snapshots), alert if it crosses the user's target.
- **Data:** `PriceHistory` (drop detection), `Watchlist`.
- **Spring:** `SnapshotService` (compare vs previous snapshot), `AlertService`, `AlertController`.
- **Marker:** `+` · **Difficulty:** Medium.

**F4 — Favourite a product type + target-price alert**
- **What:** User favourites a **type of goods** (e.g. "milk"), sets a target price (e.g. $5), and is notified when **any** item of that type is at or around that price. Uses `Price` + `Category` directly — no history needed.
- **Data:** `Product` (category), `Price`, `CategoryFavourite`.
- **Spring:** `CategoryFavourite` entity, `AlertService` (match by category + price tolerance), `AlertController`.
- **Marker:** `+` · **Difficulty:** Medium.

**F5 — Quiet hours / dedup / snooze**
- **What:** Suppress alerts during quiet hours; never repeat an already-notified price level; allow snoozing an item.
- **Data:** `AlertSetting`, `AlertLog`.
- **Spring:** `AlertService` (settings + dedup logic), `AlertSettingController`.
- **Marker:** `+` · **Difficulty:** Low.

**F6 — Morning digest**
- **What:** Summarise the day's best opportunities in a single 10-second read.
- **Data:** Latest snapshot.
- **Spring:** `DigestService` (aggregate top deals), `DigestController`.
- **Marker:** `+` · **Difficulty:** Medium.

### Shopping list, basket & budget (`+`)

**F7 — Recurring shopping lists + re-check**
- **What:** Build and save recurring lists; re-check in one tap.
- **Data:** `ShoppingList`, `ShoppingListItem`.
- **Spring:** `ShoppingListController`, `ShoppingListRepository`.
- **Marker:** `+` · **Difficulty:** Low.

**F8 — Auto-best-deal per item + basket total**
- **What:** For each list item, show the cheapest store; sum the basket (raw price total).
- **Data:** `Product` (price), `ShoppingListItem`.
- **Spring:** `BasketService` (best-deal lookup + sum), `BasketController`.
- **Marker:** `+` · **Difficulty:** Low-Medium.

**F9 — Weekly/monthly budget + over-budget warnings**
- **What:** Set a budget; running total; warn when the list exceeds it.
- **Data:** `Budget`, `ShoppingListItem`.
- **Spring:** `BudgetService`, `BudgetController`.
- **Marker:** `+` · **Difficulty:** Low.

**F10 — Category spending breakdown**
- **What:** Break down estimated spend by category (fresh food, baby, cleaning, etc.).
- **Data:** `ShoppingListItem` + `Product.category`.
- **Spring:** `BudgetService` (group by category), `BudgetController`.
- **Marker:** `+` · **Difficulty:** Low.

**F11 — Shared list / sync**
- **What:** A list shared with a partner; check-off sync, handoff. (Simplified for single-user baseline: single local list state.)
- **Data:** `ShoppingList`.
- **Spring:** `ShoppingListController`.
- **Marker:** `+` · **Difficulty:** Medium (simplified).

### Re-purchase reminders (`+`)

**F12 — Usage-rate reminders**
- **What:** Remind to re-buy based on usage rate, not a fixed schedule.
- **Data:** `Reminder` (user-set cadence).
- **Spring:** `ReminderService`, `ReminderController`.
- **Marker:** `+` · **Difficulty:** Low.

**F13 — Tap-to-add at best price**
- **What:** Tapping a reminder adds the item to the list at the current best price.
- **Data:** `Reminder`, `ShoppingListItem`, `Product` (best price).
- **Spring:** `ReminderService` (best-deal lookup + add).
- **Marker:** `+` · **Difficulty:** Low-Medium.

**F14 — Acknowledge / snooze reminders**
- **What:** Acknowledge or snooze a reminder (e.g. stop nagging for 3 months).
- **Data:** `Reminder`.
- **Spring:** `ReminderService`, `ReminderController`.
- **Marker:** `+` · **Difficulty:** Low.

### Acting on a deal & misc (`+`)

**F15 — Wishlist**
- **What:** Save items to a wishlist to track price and get alerts.
- **Data:** `Watchlist`.
- **Spring:** `WatchlistController`, `WatchlistRepository`.
- **Marker:** `+` · **Difficulty:** Low.

**F16 — Privacy export (Markdown) + delete**
- **What:** Export & download a Markdown summary/list of the user's stored data; delete local data.
- **Data:** All user-created data.
- **Spring:** `ExportController` (build Markdown, return as download), `DataController` (delete).
- **Marker:** `+` · **Difficulty:** Low-Medium.

### Accessibility & language (`+`)

**F17 — Multilingual (EN / zh-Hant / zh-Hans)**
- **What:** UI + data in three languages.
- **Data:** Language resources; multilingual product data.
- **Spring:** `MessageSource` (i18n), `LocaleResolver`.
- **Marker:** `+` · **Difficulty:** Medium.

**F18 — Senior high-contrast mode**
- **What:** Simple, high-contrast, low-effort UI mode.
- **Spring:** Frontend/theme concern; a flag.
- **Marker:** `+` · **Difficulty:** Low.

**F19 — Offline/cached usability**
- **What:** Cache snapshots for in-store use on limited data.
- **Spring:** Cache layer (Caffeine / H2-backed cache).
- **Marker:** `+` · **Difficulty:** Medium.

### Conditioned on local history-tracking (`!`)

**F20 — Local history-tracking mechanism (foundation)**
- **What:** User selects a product type (e.g. "milk"); app downloads & stores that type's daily snapshots locally for a set period, enabling comparison/trends. **First-class baseline requirement; architecture must support from day one.**
- **Data:** `PriceHistory`, `TrackedCategory`.
- **Spring:** `SnapshotService` (@Scheduled fetch), `TrackedCategory` entity, history write.
- **Marker:** `!` · **Difficulty:** Medium-High.

**F21 — Price history graphs (30/90-day)**
- **What:** Graph a product type's price over 30/90 days.
- **Data:** `PriceHistory`.
- **Spring:** `HistoryController`, charting (frontend) or aggregated data endpoint.
- **Marker:** `!` · **Difficulty:** Medium.

**F22 — Data-source + last-updated timestamp**
- **What:** Show "Consumer Council, updated <date>" so freshness is clear.
- **Data:** Snapshot date.
- **Spring:** `ProductController` (add meta), `SnapshotService`.
- **Marker:** `!` · **Difficulty:** Low.

**F23 — Threshold (% / absolute)**
- **What:** Alert on a price change of X% or a fixed $ amount; % needs a baseline from the tracked history.
- **Data:** `PriceHistory` (baseline), `Watchlist`.
- **Spring:** `AlertService` (delta logic).
- **Marker:** `!` · **Difficulty:** Medium.

**F24 — Buy-now-vs-wait advice**
- **What:** Recommend buying now vs waiting based on the trend (needs history).
- **Data:** `PriceHistory`.
- **Spring:** `AgentService` / `AdviceService` (trend + reasoning).
- **Marker:** `!` · **Difficulty:** Medium.

### Deferred (`~`)

**F25 — Voice input, sub-5s**
- **What:** Voice query for the agent.
- **Marker:** `~` · **Difficulty:** High. Low priority, implement later.

---

## 2. Ordering analysis

### 2a. By implementation difficulty

| Tier | Features | Why |
|------|----------|-----|
| **Low** | F1, F2, F5, F7, F9, F10, F12, F14, F15, F18, F22 | Simple CRUD / repo query / settings — Spring Data does the heavy lifting |
| **Low-Medium** | F8, F13, F16, F11 | A little aggregation logic (best-deal, sum, Markdown export) |
| **Medium** | F3, F4, F6, F17, F19, F21, F23, F24 | Logic + scheduling/aggregation/i18n/history — real domain work |
| **Medium-High** | F20 | Snapshot scheduling + idempotent history writes + category matching |
| **High** | F25 (agent voice) | LLM + audio + latency; deferred |

Note: the genuinely hard part in this domain is **product normalisation / size parsing** — but that is out of scope for the baseline (no structured size field), so it does not appear.

### 2b. By importance (persona value)

| Tier | Features | Why |
|------|----------|-----|
| **Core** | F1, F2 (search/compare), F4 (type target alert), F15 (wishlist) | The core "where's cheapest" value proposition |
| **Differentiator** | F24 agent (buy-now-vs-wait) + F21 (history graphs) + F3 (drop alerts) | The trust engine + agent that no competitor has |
| **High-impact / cheap** | F6 (digest), F5 (quiet hours/dedup/snooze) | Engagement without data cost |
| **Enhancing** | F7–F14 (lists/budget/reminders) | Household-manager depth, valuable but secondary |
| **Supporting** | F16–F19, F22 | Transparency, accessibility, privacy |
| **Deferred** | F25 | Low priority, later |

---

## 3. Recommended build sequence

### Phase 0 — Foundation
- **F20** local history-tracking mechanism (the snapshot fetch + `PriceHistory` write path).
- **F22** data-source + last-updated timestamp.
- **Why first:** everything read-side depends on the data being in the DB, and history needs time to accumulate — start the snapshot job on day one so the `!` features unlock later.

### Phase 1 — Core read path
- **F1** fuzzy search, **F2** side-by-side comparison.
- **Why:** this is the visible core; get something on screen fast.

### Phase 2 — The agent (differentiator)
- **F24** buy-now-vs-wait advice + the natural-language "where's cheapest" endpoint (LLM).
- **Why:** the agent is the unique value; it sits on the search/compare read path from Phase 1.

### Phase 3 — Alerts & engagement
- **F3** drop alerts, **F4** type target-price alert, **F5** quiet hours/dedup/snooze, **F6** digest.
- **Why:** alerts drive retention and reuse the snapshot/history path. F4 is the standout "milk at $5" feature — high value, needs no history.

### Phase 4 — Lists, basket & budget
- **F7** recurring lists, **F8** auto-best-deal + basket total, **F9** budget + warnings, **F10** category breakdown, **F11** shared list.
- **Why:** household-manager depth; reuses price lookups already built.

### Phase 5 — Reminders
- **F12** usage-rate reminders, **F13** tap-to-add at best price, **F14** acknowledge/snooze.

### Phase 6 — History-gated trust (unlocks as data accumulates)
- **F21** history graphs, **F23** threshold %/absolute.
- **Why:** only useful after Phase 0 has accumulated snapshots.

### Phase 7 — Supporting & accessibility
- **F16** privacy export (Markdown), **F17** multilingual, **F18** senior mode, **F19** offline cache.

### Phase 8 — Deferred
- **F25** voice input (low priority, later).

---

## 4. Ordering rationale

- **Data first (Phase 0):** the app is a price app; without data there is nothing. Snapshot accumulation must start immediately because history-gated features need time to build up.
- **Core read path early (Phase 1):** search + compare is the minimum visible value and unblocks everything else.
- **Differentiator next (Phase 2):** the LLM agent is what makes this app unique, not another price listing site.
- **Engagement (Phase 3):** alerts reuse the snapshot path and drive retention; F4 (type target price) is cheap and high-value.
- **Depth (Phase 4–5):** lists/budget/reminders add household-manager value on top of already-built price lookups.
- **History-gated (Phase 6):** these wait for accumulated data by design; they are in scope but not day-one deliverables.
- **Supporting (Phase 7) and deferred (Phase 8):** transparency/accessibility are important but not blocking; voice is later.

---

## 5. Testability checkpoints

- **After Phase 0:** a scheduled fetch populates `Product` + `PriceHistory`; timestamps visible.
- **After Phase 1:** search + comparison endpoints return correct grouped results.
- **After Phase 2:** agent returns a reasoned single answer.
- **After Phase 3:** alert fires on drop / type target match; quiet hours + dedup work.
- **After Phase 4–5:** lists, budgets, reminders behave.
- **After Phase 6:** history graph and % threshold work once snapshots exist.

---

## 6. Out of scope (not built in baseline)

Unit-price comparison, product normalisation, delivery/in-store fees, full-basket comparison, out-of-stock flags, enrichment/reviews, original+sale+% saving, fake-discount detection, offer age/expiry, bulk (storage-aware) advice, "wait was wrong" acknowledgment, multiple channels by urgency/flash-sale, basket inflation tracking, bulk-buy flags, one-tap buy link, WhatsApp/coupon share, eligibility tags, "money saved" counter, cost-change vs week/month. These require data the baseline does not have.