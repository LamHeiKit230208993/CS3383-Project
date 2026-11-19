# TC-HISTORY-001 — Product observation history with explicit gaps

This case covers decision D13's history view. Dated execution results are
recorded separately in the test history.

Sources:
[`ProductHistoryControllerTest`](../../../src/test/java/com/pricewatch/web/ProductHistoryControllerTest.java),
[`ProductHistoryService`](../../../src/main/java/com/pricewatch/tracking/ProductHistoryService.java),
[`history.html`](../../../src/main/resources/templates/pages/history.html), and
the history scenario in `src/test/browser/milestone-check.cjs`.

## Access

`GET /history/{code}` is available only for a product with at least one tracking
period. An unknown code and a known-but-untracked product both return 404.

`/saved` shows a **View history** link for each tracked product; it links to the
same product code. The history page links back to `/saved`.

## Table content

Each tracking period is shown separately with its retailer, tracking-since time,
stopped time when present, target price, drop-rule state, and enabled/stopped
status. Observations are listed with:

- the retrieval timestamp, formatted in Hong Kong time;
- the listed price, or `Unknown` when the stored observation is an explicit
  unknown;
- a gap row when the interval between adjacent observations is at least seven
  days. The gap row states the number of days with no recorded observation.

Only accepted live observations inside the 90-day retention window and not
after the current clock instant are shown. Filtering happens before the gap
scan, so an expired observation cannot create a gap around visible data. The
view never invents daily values, source price dates, or prices for untracked
periods.

## Empty states

A tracked product with no retained observations shows **No recorded
observations.** and explains that this product is tracked but no accepted live
import recorded a price in the last 90 days. Each period also has its own
empty-state message.

## Browser evidence

The milestone scenario follows **View history** from `/saved` for the tracked
product, checks the title, retailer, retrieval/list-price headings, a currency
amount, the 90-day retention label, and the no-invented-values note. It checks
that an unknown product returns 404 and that the page fits 375 px and 1440 px
without horizontal overflow.

## Limits

The page is a read-only view, not an editable history or a price forecast.
Timestamps are when this app retrieved the data, not a source price date.
Retention is 90 days. The Java tests use a mutable test clock and direct
observation rows, so they do not prove a live feed will contain a seven-day gap.
The browser check uses Chromium and same-day imports; gaps and retention are
covered by the Java test with controlled timestamps.
