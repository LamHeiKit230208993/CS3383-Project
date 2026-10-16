# Persona Requirements — Family / Household Manager

## Persona Summary

A household manager in Hong Kong, working full-time with two kids (ages 4 and 7) and a husband. Shops for groceries and household goods roughly weekly for fresh items plus a monthly "top-up" run for bulky staples. Tracks what the household runs out of and does most of the purchasing. Wants the app to save time, prevent wasteful over-buying, and catch good deals without constantly scrolling.

---

## Requirements

### Planning & Shopping Cadence

- Plan a weekly grocery list that syncs to an in-store trip, checkable off while shopping.
- Remember the typical purchase cycle (fresh veggies weekly, rice/oil monthly, detergent ~every 6 weeks) and suggest when each item is due.
- Support bulk shopping — flag when a big-pack price-per-unit is cheaper while already at the store.
- Record prices actually paid at usual stores, so the app learns real costs rather than only advertised ones.

### Regular Purchases / Staples

- Build a "frequent items" list (baby formula, diapers, toilet paper, laundry detergent, cooking oil, rice, fresh eggs, milk) with tracked price history and surfaced deals.
- Distinguish must-buy-specific-brand items (a brand the kids tolerate) versus interchangeable generics, so it doesn't recommend swaps that won't work.

### Budget Management

- Set weekly and monthly household budget; see estimated running total against it as items are added.
- Warnings when a planned basket is about to exceed the budget, with suggestions to drop or substitute.
- Category breakdown of spending (fresh food, baby, cleaning, toiletries) to spot what's creeping up.

### Shared Shopping List

- Share a shopping list with a partner so either can add items or check them off, and see who added what.
- Conflict resolution: avoid duplicate purchases via near real-time check-off sync when both shop at different stores.
- "Who's shopping" handoff — the partner going to the store takes the list; the other stops editing to avoid conflicts (or edits are merged).

### Basket / Inflation Tracking

- Track a fixed "basket" of regular items and see its total price over time to tell if inflation is hitting the household.
- Use actual paid prices, not just the cheapest advertised.
- Price history charts per item and per basket, with clear up/down indicators.

### Agent Advice (Buy Now vs Wait)

- Agent tells "buy now" vs "wait" based on whether the current price is a genuine low versus normal fluctuation.
- Recommend "buy in bulk now" when a staple is at a historical low and there's storage space; warn against hoarding.
- Warn when a "discount" is not actually a good deal compared to the normal paid price (misleading sales).
- Advice accounts for storage constraints — can't bulk-buy fresh food, but can stock up on rice, detergent, canned goods.
- Agent explains its recommendation briefly ("cheapest in 3 months, buy 2") so it's trusted.

### Re-purchase Reminders

- Reminders to re-buy items about to run out, based on usage rate rather than a fixed schedule.
- Suggest buying now if there's a deal, or waiting if prices are expected to stay flat.
- Easily acknowledge/clear a reminder and snooze it ("not needed yet, remind me next week").
- Reminders are actionable — tapping one adds the item to the shopping list with the best current price.

---

## Notable Pain Points / Edge Cases

- **Manual price-comparison fatigue** — currently opens multiple store apps and does the math manually.
- **Advertised prices aren't what's paid** — sales are often out of stock or the discount doesn't apply to the size needed.
- **"Discount" distrust** — burned by fake sales; needs a historical baseline to compare against.
- **Budgeting is reactive** — only realizes overspent at month end; wants proactive warnings as the basket is built.
- **Shared-list sync** — both partners sometimes double-buy; real-time sync and a clear "owner" would fix it.
- **Storage reality** — bulk advice is useless if it ignores limited pantry/fridge space.
- If a recommended store is out of stock or the offer expired, flag it and offer next-best rather than showing a stale deal.
- Simultaneous edits to the same list must not overwrite each other (merge or lock).
- If a "wait" recommendation is wrong and price later rises, want an honest acknowledgment.
- In-store with limited data — list and prices should be usable offline/cached.
- Price-history baselines need enough data points; early on, say "not enough history yet" rather than give shaky advice.
- Substitutions must respect brand constraints and allergen/health notes for the kids (no nuts, specific formula).