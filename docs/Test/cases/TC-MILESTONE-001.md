# TC-MILESTONE-001 — J05–J08 milestone browser flow

## Purpose

Check the integrated saved-product, retailer tracking, alert, budget, and
replenishment-reminder pages with a real browser and a local synthetic feed.

## Setup

- Build the executable JAR from the repository root.
- Run `node src/test/browser/milestone-check.cjs` with Playwright and Chromium
  available.
- The script starts a local two-product HTTP feed and a separate Spring Boot
  process. It uses a per-process H2 in-memory database and does not use the
  normal `./data/pricewatch` file.

## Steps and expected results

1. Import the synthetic live feed. Save one product to the wishlist and verify
   that its first saved state does not start tracking. Start tracking it for a
   retailer as a separate action.
2. Set a target and a drop rule. Set quiet-hour start and end to `00:00`, which
   disables the quiet window. Change the feed price and refresh it. The alert
   page shows both the target and consecutive-price-drop events from the new
   observation.
3. Create a quantity and interval reminder for the second product. Add its
   quantity to My list, then clear and snooze it. Adding does not clear the
   reminder; clear recalculates its next interval; snooze changes the current
   due time by 24 hours.
4. Add the first product to My list and save a monthly budget. The displayed
   known estimate and category totals reflect the current list prices.
   Refresh a second lower tracked price with both items already in the list.
   The selected retailer stays pinned, the list and budget totals change, and
   the still-qualifying target does not repeat.
5. Send invalid reminder, target-price, and budget parameters. They return a
   validation redirect or a client error; none returns HTTP 500.
6. Advance the browser clock on a clean active page. It reloads after one minute.
   Edit a reminder form and move focus away. Advance the clock and verify that
   there are no navigations and that the entered value stays unchanged.
7. Check `/saved`, `/alerts`, `/budget`, and `/reminders` at 375px and 1440px.
   Each route has one main heading and no horizontal overflow; short alert prices
   stay on one line. The script saves
   desktop/mobile screenshots and `app.log` to `target/browser-milestone/`.
8. Open all four pages with JavaScript disabled and submit a budget change.
   Server-rendered pages and native form validation/redirects still work.

## Automated source

`src/test/browser/milestone-check.cjs`

## Limits and review notes

The feed is generated locally and does not represent live shop prices. Running
the script requires a packaged JAR. Syntax validation alone does not prove the
browser flow passed; record the run result in test history after execution.
