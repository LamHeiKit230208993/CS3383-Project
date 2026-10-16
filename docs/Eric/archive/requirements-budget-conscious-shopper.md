# Persona Requirements — Budget-Conscious Shopper

## Persona Summary

A working professional living in a small HK flat, on a fixed monthly grocery/household budget. Uses the app on mobile, mostly in the evening (commute + before-bed). Wants the *cheapest correct thing* fast, but doesn't trust "cheap" blindly. Not a coupon-clipper by personality — wants low friction and a "set and forget" alert system. Values unit pricing and honesty over flashy deals.

---

## Requirements

### Finding the cheapest price

1. Compare the same product (same brand, same pack size) across all participating retailers in one view — not one search result per store.
2. Show **unit price** (per 100g / per L / per piece) as the primary sort key, since pack sizes vary.
3. Normalise the product identity — group the "same" product even if listed under different names or pack sizes across stores.
4. Include **delivery vs in-store pickup** pricing distinctly.
5. Show **delivery minimums and fees**, plus estimated delivery time.
6. Let the user compare **one item across stores AND a full basket**.
7. Flag **out-of-stock / unavailable** items clearly.

### Trusting the "best deal" recommendation

8. Show the **price history** for an item.
9. Show the **reference/regular price** next to the sale price to see the actual % saving.
10. Tell the user **how long the discount has been running** and whether it's periodic vs permanent.
11. Show **when the price last changed and the source/date** of the data (data freshness).
12. Give a reason per recommendation ("cheapest unit price", "lowest delivered total", etc.).
13. Show **review/quality signals** so cheapest isn't assumed best.
14. Distinguish **"currently cheapest"** from **"historically cheap / good time to buy"**.

### Deal alerts

15. Let the user pick **specific items** to track (staples: rice, toilet paper, cooking oil, detergent) — not a blanket "all deals" alert.
16. Let the user set a **target price per item** and only alert when the price drops below it.
17. Alert on **% saved vs a minimum absolute threshold** — "HKD 20 off my usual rice" not "HKD 1 off a snack".
18. Let the user set **alert frequency and quiet hours**; allow a daily digest.
19. Scope alerts to **a specific store** (the one near the flat).
20. Include **stock/expiry awareness** — don't alert on perishables that can't be used in time, nor bulk deals with no storage space.
21. Allow **temporary snooze** on an item.

### Budget-planning workflow

22. Support a **weekly** budget cycle with a monthly view.
23. Track **actual spend** per item/store so the app can flag budget overruns.
24. Tell the user **when buying in bulk is actually cheaper** per unit vs small, accounting for storage and expiry.
25. Flag **"buy now vs wait"** based on historical lows.
26. Set a **total weekly spend target** and warn before blowing it on a bad "deal".

### Retention (why they'd keep using it)

27. **Speed** — one-tap comparison; faster than checking two websites.
28. **Frictionless alert setup** — default sensible categories, refine later.
29. **Accuracy over breadth** — correctly-matched, up-to-date catalog beats a big stale one.
30. **Basket saving** — save a recurring shopping list and re-check in one tap.
31. **Price history chart** — the most trust-building feature.
32. **Clear data-source label** ("Consumer Council, updated <date>").
33. Optional **bundle/savings nudges** ("buy 2 for 10% off") only when they genuinely beat per-unit price.

---

## Notable Pain Points / Edge Cases

- **Duplicate/mismatched listings** (same product, different names/pack sizes) → untrustworthy comparison.
- **"Cheapest" trap** — lowest unit price isn't the cheapest once delivered/in-stock is factored in.
- **Stale data** — daily updates mean freshness matters.
- **Alert fatigue** — needs per-item thresholds, not blanket alerts.
- **Storage & expiry reality** — a great bulk deal is worthless in a small HK flat.
- **Not knowing the baseline** — "40% off" is meaningless without regular price + history.
- **Hidden delivery fees & minimums** — breaks trust.