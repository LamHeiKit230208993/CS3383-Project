# UI prototype — Market Ledger theme

Status: UI prototype for issue #7. This is not a completed baseline job and does
not prove real data import, storage, search, history, alerts, or AI.

## Theme choice

**Market Ledger** is a warm, editorial price ledger for Hong Kong household
shopping. Cream paper surfaces, deep teal ink, coral sale cues, and green
best-price cues echo local market price tags without copying a retailer brand.
Serif headings and price numerals make the comparison feel trustworthy and easy
to scan; a humanist sans body keeps dense retailer rows readable.

The prototype is deliberately honest: a persistent banner says **UI prototype ·
Sample data**, and every price, date, and quantity is marked as sample. Listed
prices are not checkout prices, and delivery, stock, and branch data are not
included.

## Design tokens

| Token | Value | Use |
|---|---|---|
| `--paper` | `#fbf6ec` | Page background |
| `--paper-2` | `#f3ebdc` | Secondary surfaces |
| `--surface` | `#fffdf8` | Cards and panels |
| `--ink` | `#17323b` | Primary text, dark panels |
| `--ink-soft` | `#2c4d57` | Secondary text |
| `--muted` | `#6f7d86` | Captions and metadata |
| `--line` | `#e6dcc7` | Hairlines |
| `--coral` | `#d9402f` | Sale, urgency, primary action |
| `--coral-dark` | `#b33526` | Hover and pressed |
| `--green` | `#1f7a5c` | Best price and savings |
| `--green-pale` | `#e7f1ea` | Best-price row |
| `--amber` | `#c98a1a` | Budget and tie warnings |

- Headings and prices: `Georgia`, with a serif fallback. No external font is
  loaded, so the prototype works without a network.
- Body: `Segoe UI`, system UI, `Noto Sans`, Arial.
- Prices use tabular numerals.
- High-contrast mode swaps to white surfaces, near-black text, stronger
  borders, and deeper green/coral.
- Meaning never relies on colour alone: badges and text accompany every cue.

## Screen flows

- **Search** — search input, category filters, sample product cards, no-results
  state, and a recent comparison card.
- **Compare** — exact product and pack, retailer rows with best/tie/missing
  states, original offer text, source and retrieval dates, quantity dialog, and
  add-to-list demo.
- **List** — planned budget, quantities, checked state, per-item best retailer,
  estimated total, and an explicit “not actual spending” label.
- **Saved** — wishlist and tracking are separate; saving alone does not start
  tracking.
- **Alerts** — target-price and drop alerts, quiet hours, repeat prevention,
  snooze, reminder interval, and an in-app morning digest.
- **History** — sample chart with available dates, gap note, accessible value
  table, “not enough history” empty state, and a “not a prediction” note.
- **Ask** — sample conversation with a checked comparison, clarification note,
  suggestion chips, and a canned demo reply.
- **Settings** — language preview, high-contrast toggle, privacy/offline notes,
  quick links, and the Hello fallback page.
- **Hello fallback** — the original Hello page is kept at `/hello`.

## Prototype limits

- All product, price, history, alert, and assistant content is sample data.
- Demo state stays in browser memory and resets on reload.
- There is no database, price import, REST data endpoint, live AI call,
  notification service, account, or cloud saving.
- Language support is a preview of a few labels, not a full translation.
- High-contrast mode is a display preview, not a completed accessibility audit.

## Run and evaluate

```powershell
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080`. It redirects to `/search`. The Hello fallback is at
`http://localhost:8080/hello`.

Suggested checks:

1. Search for “milk”, “rice”, and a Chinese sample name; clear the search.
2. Open a product, inspect best/tied/missing rows, open the quantity dialog, and
   add a sample item.
3. Change list quantities and check items; confirm the estimated total updates
   and is labelled as an estimate.
4. Visit Saved, Alerts, History, Ask, and Settings through the visible
   navigation.
5. Toggle high contrast and the language preview in Settings.
6. Use only the keyboard; check focus visibility, the skip link, and dialog
   Escape behaviour.
7. Check widths at 375, 768, 1024, and 1440 pixels; confirm no horizontal scroll
   and that mobile navigation does not hide content.
8. Reload; confirm demo state resets and the sample-data banner remains.
