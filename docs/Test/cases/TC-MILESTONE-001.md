# TC-MILESTONE-001 — J05–J08 milestone browser flow

## Purpose

Check the integrated saved-product, retailer tracking, alert, budget, and
scheduled-rule pages with a real browser and a local synthetic feed.

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
2. Seed a legacy product-wide target and a retailer-specific drop rule through
   their existing routes. Refresh with two qualifying prices.
   Alerts shows one grouped target and the existing drop evidence.
3. Create a weekly calendar reminder. Verify the Current rules card shows the
   product, cadence, and type. Pause it there and verify the product editor
   shows the same Off state. Turn it back on, change the cadence to every two
   weeks, and delete it in the item editor. Also toggle an earlier retailer
   rule from Current rules.
   The due occurrence, snooze, dismissal and retained history have separate
   [controlled-clock checks](TC-REMINDER-001.md).
4. Add both products to Cart and save a monthly budget in Personal data.
   The displayed estimate reflects current Cart prices.
   Refresh a second lower tracked price with both items already in the list.
   The selected retailer stays pinned, the list and budget totals change, and
   the still-qualifying target does not repeat. Raise the prices above the
   target, cross the target again, and repeat the final refresh. Check there
   are exactly two grouped events.
5. Seed a second legacy price rule and a combined price-and-time rule for
   the same pack. Edit the interval, verify the exported settings and the
   first rule's existing history, then use the keyboard to open and delete the
   combined rule at mobile width. A controlled server test checks what happens
   when the combined occurrence is due.
6. Open Set rules from Wishlist. Add a one-time price condition and a weekly
   calendar condition in the same dialog. Check the saved time and recurrence.
   Open the same editor from Current rules, switch the calendar rule off and
   back on, then delete it. Trigger the one-time price alert with the fixture
   feed and refresh again. Verify exactly one event and a completed, disabled
   rule. Open the editor again from that History entry.
7. Send invalid reminder, target-price, and budget parameters. They return a
   validation redirect or a client error; none returns HTTP 500.
8. Advance the browser clock on a clean Alerts page. It reloads after one minute.
   Open the editor, enter a draft price, and move focus away from the form.
   Advance the clock and verify that the page and draft survive.
9. Check Wishlist, both Alerts views, Personal data's Budget card, and item rules at
   375px and 1440px, including the links from the old routes.
   Each route has one main heading and no horizontal overflow; short alert prices
   stay on one line. The script saves
   desktop/mobile screenshots and `app.log` to `target/browser-milestone/`.
10. Open the milestone pages without JavaScript and submit a budget change.
    The Wishlist rules link opens the server-rendered editor.

## Automated source

`src/test/browser/milestone-check.cjs`

## Limits and review notes

The feed is generated locally and does not represent live shop prices. Running
the script requires a packaged JAR. Syntax validation alone does not prove the
browser flow passed; record the run result in test history after execution.
