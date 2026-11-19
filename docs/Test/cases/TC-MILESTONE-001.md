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

1. Import the synthetic live feed, save one exact pack to Wishlist, open the
   row, and verify tracking is already visible in its history.
2. Set a product-wide target from the Wishlist row and seed a retailer-specific
   drop rule through its legacy route. Refresh with two qualifying prices.
   Alerts shows one grouped target and the existing drop evidence.
3. Create, edit, and delete a time reminder in the second item's rule panel.
   The due occurrence, snooze, dismissal and retained history have separate
   [controlled-clock checks](TC-REMINDER-001.md).
4. Add both products to Cart and save a monthly budget in Personal data.
   The displayed estimate reflects current Cart prices.
   Refresh a second lower tracked price with both items already in the list.
   The selected retailer stays pinned, the list and budget totals change, and
   the still-qualifying target does not repeat. Raise the prices above the
   target, cross the target again, and repeat the final refresh. Check there
   are exactly two grouped events.
5. Send invalid reminder, target-price, and budget parameters. They return a
   validation redirect or a client error; none returns HTTP 500.
6. Advance the browser clock on a clean active page. It reloads after one minute.
   Edit a reminder form and move focus away. Advance the clock and verify that
   there are no navigations and that the entered value stays unchanged.
7. Check Wishlist, Alerts, Personal data's Budget card, and item rules at
   375px and 1440px, including the links from the old routes.
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
