# Persona Requirements — Deal Hunter / Sale-Watcher

## Persona Summary

Deal hunter (28, Hong Kong), avid bargain-shopper on a budget. Checks the app daily during lunch and before sleep. Hoards discounted household staples and electronics. Deeply skeptical of "fake" discounts.

---

## Requirements

### Discovery & Browsing

- Browse by category (food, electronics, household) AND by store brand, not just search.
- Search matches fuzzy terms ("iphone 15", "iphone15") and handles both English + traditional Chinese names.
- A "trending now" feed of deals other users are watching, refreshed by time-of-day (lunch = food deals, night = electronics).
- Quick filter for recently viewed/saved items to resume shopping.

### Deal Quality & Trust

- Show ORIGINAL (pre-discount) price AND sale price together; hide nothing.
- Show historical price graph (last 30/90 days) to judge if the "deal" is a real drop.
- Flag "fake discount" (sale price equals or is above recent average) with a warning badge.
- Show how many days the offer has been running and when it expires.
- Mark expired offers as expired rather than silently removing them.

### Notifications

- Push notifications, not just in-app.
- Per-item "price drop alert" — notify only when a saved item drops below MY target price, not every small fluctuation.
- Time-window control; quiet hours 11pm–8am.
- Separate channels: email for weekly digest, push for urgent flash sales, in-app badge for low-priority.
- Deduplicate — no repeated alerts for the same price level already seen.

### Flash Sales & Urgency

- A dedicated "flash sale" section with a live countdown.
- Alert 30 min before a flash sale ends for wishlist items.
- Near-realtime updates (poll or push) for countdowns, rather than stale daily data.

### Acting on a Deal

- One-tap link to the store page to buy immediately.
- "Save to wishlist" to track price and get alerts.
- "Share deal" button (WhatsApp is king in HK).
- Copy coupon code with one tap.
- Show whether the deal is online-only, in-store-only, or member-only before wasting a trip.

### Member & Coupon Logic

- Surface member-only deals separately and show required membership level.
- Track coupon codes and show whether stackable with the current sale.

### Price Tracking & History

- Save an item and see its price history over time even if no active offer.
- Detect "bait" pattern (price raised just before a "sale") — warn if discounted price is still above what it cost 60 days ago.

### Edge Cases

- Price may differ between branches / online vs physical store — show store-specific prices.
- Some deals require being physically at the store; don't show online-buy links for those.
- Handle stale offers (show last-updated timestamp).
- Products that disappear from catalog (out of stock / discontinued) — keep history but mark status.

---

## Notable Pain Points

- Burned by "sales" that aren't real discounts; without historical price, can't tell genuine deal from markup.
- Daily-only data makes flash sales feel laggy.
- In-app-only alerts mean they're never seen unless the app is opened.
- Over-notification trains them to ignore alerts; only targeted price-drop triggers work.
- Not knowing expiry/eligibility (member-only, in-store-only) wastes trips or clicks dead links.