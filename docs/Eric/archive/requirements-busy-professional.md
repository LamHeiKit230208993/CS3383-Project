# Persona Requirements — Busy Professional

## Persona Summary

"Kwan" — 39, works 60+ hrs/wk at a fintech firm in Central, commutes 40 min each way, does most household/grocery shopping on the fly between meetings. Wants to spend money but not *time* thinking about where. Time-poor, decision-fatigued, phone in one hand, coffee in the other.

---

## Requirements

### Interaction & Speed

- Talk to the app (voice) while walking between meetings — no reading or typing; reply in under 5 seconds.
- Ask in plain Cantonese/English mixed, like "I'm out of olive oil, where's cheapest today" and get ONE answer — a store name + price + why, not a ranked table.
- One-tap "just buy it for me" — the app already knows what is usually bought; one button re-orders/points there without re-asking preferences.
- Agent gives the recommendation AND one sentence of reasoning ("Cheapest today, $38 at ParknShop, 20% off usual").
- Works offline-ish / instant even on spotty MTR wifi — no network round-trip for a simple "where's X cheapest".

### Personalization

- Recommendation personalized to actual buying history (brands bought, quantities, usual store) — not generic "cheapest" that might send to an unfamiliar chain.
- Set "never buy" boundaries — never recommend a disliked brand, never a store with a bad experience.
- Handle ambiguous queries like "milk" — ask ONE clarifying question or default to past purchase, not dump 40 results.

### Alerts & Digests

- Price alerts pushed as notifications (not email); must be genuinely useful or will be muted — e.g. a real drop (>15%) on a brand already bought, not a daily noise blip.
- A single morning digest (8am, before leaving) summarising "3 things worth knowing today" that fits in one 10-second read, no deep-linking required.
- Quiet-hours setting — no pings during meetings.
- Geolocation-aware alerts: if already near a store that has a wanted item on offer, tell them NOW.

### Buying & Navigation

- Buy directly from the app, or get a walking route + estimated time to the store.
- If the usual brand is out of stock everywhere, suggest the next-best tolerated brand with a note it's a substitution.
- Consider the commute/trip in recommendations — not just absolute price (a slightly pricier store on the route home beats a 30-min detour).

### Trust & Value

- Show the source (Consumer Council data date) and last-updated timestamp so it's known to be today's price, not stale.
- Show how cost changed vs last week/month — a "money saved" counter proving the app's value.

### Minimum to keep installed

(a) one-tap voice purchase, (b) the daily 10-second digest, (c) the geo-reminder when near a store. Anything else is decoration.

---

## Notable Pain Points / Edge Cases

- **Alert fatigue is the #1 killer** — the moment notifications become noise, the app is muted and dead. Relevance beats frequency.
- **Time-to-answer must be sub-5s** — a slow chatbot is useless mid-stride.
- **"Cheapest" isn't enough** — wants *best-for-me* given route, brand loyalty, and past experiences, requiring personalization not raw price ranking.
- **Voice is essential** but must work in noisy Cantonese street/commute audio or it's a gimmick.
- **Doesn't want to think** — the app should make the decision and only surface exceptions, not present options to weigh.

### Privacy considerations

- Comfortable with personalization IF it's clearly local and history can be seen/erased — a "what you've told me" audit screen.
- Wary of constant location tracking; geo-alerts must be explained ("we use location only when you open the app or enable trip mode") with a visible toggle.
- Doesn't want purchase habits sold to advertisers; would pay a small subscription to avoid that.
- Wants to know exactly what's stored and export/delete it in one tap.