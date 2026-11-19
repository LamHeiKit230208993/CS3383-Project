# TC-MILESTONE-001 — J05–J12 milestone browser flow

## Purpose

Check the integrated saved-product, retailer tracking, alert, budget,
replenishment-reminder, Ask fallback, list-transfer, language/contrast,
history, privacy, and offline-snapshot flow with a real browser and a local
synthetic feed.

## Setup

- Build the executable JAR from the repository root.
- Run `node src/test/browser/milestone-check.cjs` with Playwright and Chromium
  available.
- The script starts a local two-product HTTP feed and a separate Spring Boot
  process. It uses a per-process H2 in-memory database and does not use the
  normal `./data/pricewatch` file.

## Steps and expected results

1. Import the synthetic live feed. Save one product to the wishlist and verify
   that its first saved state does not start tracking. Use the single
   **Track this product** action as a separate choice.
2. Set one product target and a retailer drop rule. Set quiet-hour start and
   end to `00:00`, which disables the quiet window. Change the feed price and
   refresh it. The alert page shows both the target and consecutive-price-drop
   events from the new observation.
3. Create a quantity and interval reminder for the second product. Add its
   quantity to My list, then clear and snooze it. Adding does not clear the
   reminder; clear recalculates its next interval; snooze changes the current
   due time by 24 hours.
4. Add the first product to My list and save a monthly budget. The displayed
   known estimate and category totals reflect the current list prices.
   Refresh a second lower tracked price with both items already in the list.
   The selected retailer stays pinned, the list and budget totals change, and
   the still-qualifying target does not repeat.
5. Ask for a product with no API key. The checked answer uses the safe fallback
   and shows the checked price and retailer. An ambiguous question shows a
   chooser; choosing the second product shows its checked price.
6. Export My list and verify the versioned attachment contains the saved
   product, quantity, and retailer. Import a file for the second product. Expect
   a separate list preview and its records. My list does not gain that product.
   Import malformed JSON and expect an error with the earlier list still present.
   Check `/list/import` at 375px and 1440px with no horizontal overflow.
7. Send invalid reminder, target-price, and budget parameters. They return a
   validation redirect or a client error; none returns HTTP 500.
8. Advance the browser clock on a clean active page. It reloads after one minute.
   Edit a reminder form and move focus away. Advance the clock and verify that
   there are no navigations and that the entered value stays unchanged.
9. Check `/saved`, `/alerts`, `/budget`, and `/reminders` at 375px and 1440px.
   Each route has one main heading and no horizontal overflow; short alert prices
   stay on one line. The script saves
   desktop/mobile screenshots and `app.log` to `target/browser-milestone/`.
10. Open all four pages with JavaScript disabled and submit a budget change.
    Server-rendered pages and native form validation/redirects still work.
11. Switch to Traditional Chinese and high contrast. Check `<html lang>`,
    `data-contrast="high"`, the body text contrast ratio (at least 7:1), a
    visible status-pill border, and the 4 px keyboard-focus outline. Check
    `/budget` at 375, 768, 1024, and 1440 px, then restore English/normal.
12. Follow **View history** from `/saved` for the tracked product. Confirm the
    retailer, retrieval/list-price headings, a known amount, the 90-day
    retention label, and the no-invented-values note. An unknown product
    returns 404, and the page fits 375 px and 1440 px.
13. Open `/privacy` and download the versioned personal-data JSON. Confirm it
    contains lists, wishlist, tracking observations, alerts, budget, reminders,
    and preferences, and no catalogue section. Submit the delete form without
    confirmation and confirm nothing changed; confirm deletion and verify the
    personal counts are zero and the accepted product still opens. Re-add the
    two products to My list for the offline check.
14. Download `/list/offline` for My list and confirm the attachment contains the
    saved products, quantities, retailers, unit and line amounts, saved-on time,
    data-age label, and unknown source price date. Stop the Spring Boot process,
    then open the saved `.html` file in an offline browser context. The same
    values remain visible, the page has no script, form, link, or resource URL,
    and the layout fits 375 px and 1440 px.

## Automated source

`src/test/browser/milestone-check.cjs`

## Limits and review notes

The feed is generated locally and does not represent live shop prices. Running
the script requires a packaged JAR. The script has no AI API key, so Ask covers
the deterministic fallback, not a live model answer. List transfer and the
offline snapshot use the synthetic feed and in-memory database. The offline
check opens a `file://` page in an offline browser context after stopping the
app; it does not test a real phone or every browser. Syntax validation alone
does not prove the browser flow passed; record the run result in test history
after execution. The contrast check measures computed colours and focus outline
in Chromium; it is not a full WCAG audit. Privacy deletion removes the user's
preferences, so the scenario re-adds list items before the offline check. The
synthetic feed has no seven-day observation gap; gap and retention behaviour is
covered by `ProductHistoryControllerTest`.
