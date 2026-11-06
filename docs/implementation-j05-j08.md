# Market Desk: J05–J08 implementation

Updated: 6 November 2026. Local implementation and integration checks passed.
The changes await commit/PR review and merge; this is not an issue-closure or
release claim. Use the [test catalogue](Test/test-case-catalog.md) and
[dated history](Test/history.md) for observed results.

Read the [J01–J04 baseline](implementation-baseline-j01-j04.md) for source parsing,
search, exact money, retailer choice and list rules. The
[milestone plan](Plan/tracking-alerts-budgets-reminders-j05-j08.md) records D01–D07,
agreed with the user on 6 November 2026. The [project scope](project-scope.md)
still controls feature decisions.

## What the extension adds

The flow is **save a product → explicitly track a retailer → accept a live price
change → review an alert → compare the list with a planned budget → act on a
reminder**. Each action can also be used independently.

The app remains one local Spring Boot process with Thymeleaf forms, plain
HTML/CSS/JavaScript, Spring Data JPA and an H2 file database. It has no React
frontend, separate API server, accounts or cloud storage.

| Area | Stored state | Application owner |
|---|---|---|
| Wishlist | Exact product and saved time | `TrackingService` |
| Tracking | Product, retailer, enabled period, target/drop rules and repeat state | `TrackingService` |
| Observations | Tracking period, nullable price, retrieval time, source URL and checksum | `TrackingService` |
| Alerts | Target/drop evidence, generation/availability/read/snooze state, daily summary | `AlertService` |
| Timing | Quiet start/end and summary time | `NotificationPolicy` |
| Budget | Weekly/monthly choice and planned limit | `BudgetService` |
| Reminders | Product, packs, interval, optional retailer, due/snooze state | `ReminderService` |

## Saving and tracking

Saving a wishlist entry does not start tracking or add a shopping-list item.
Tracking needs an exact product and an explicitly chosen retailer. It does not
change the retailer saved in My list. Each enabled period collects only its own
product/retailer observations.

An eligible accepted LIVE catalogue can seed the first observation. Subsequent
successful LIVE imports record changes, including a transition to an unknown
price. Consecutive unchanged prices and repeated unknown values add no new
observation. A changed retrieval time alone does not prove a changed price.
SAMPLE imports do not seed live history or produce live price alerts.

The app retains observations for 90 days. A small last-value state remains on
the period so expiry does not turn an unchanged re-import into invented history.
Expired observations are pruned when history is read and on live imports.
Stopping tracking keeps existing history and stops collection. Starting again
creates a separate period; its drop rule cannot compare with the old period.
Explicit history deletion removes that period's observations and price alerts,
without removing the wishlist entry or shopping-list item.

Catalogue replacement, observation writes and price-rule evaluation use the
same transaction. An import failure must roll back all three. History does not
depend on the lifetime of retailer listings, which are replaced by imports.
The source price date remains unknown; stored observation times are retrieval
times. Alert generation time is separate.

## Alerts and time

A target qualifies at **known price <= target**. The first eligible known price
can create one event. It rearms after a later known price rises above the target.
Saving the same target again does not create a new event. A blank target removes
that rule. Target evaluation must use eligible current LIVE data, not SAMPLE
data or an expired accepted catalogue. A fresh accepted price can meet a target
even when unchanged older observations have expired; it does not create a fake
history row.

A drop needs two adjacent known observations in the same period, with the newer
price lower than the earlier price. An unknown gap breaks the comparison. Drop
events keep both prices and their retrieval times, separate from event generation
time. There is no configurable amount/percentage threshold, multi-buy price
calculation, or buy/wait advice.

All timing uses `Asia/Hong_Kong` through a shared injectable `Clock`. Defaults:

- Quiet hours: 22:00–08:00. The start is included and the end is excluded.
- Daily summary: 08:00. At most one summary is created per Hong Kong date, from
  unread, unsummarised price events that are eligible at the check time.
- Alert snooze: 24 hours from the action. Read and snooze state are persisted.

Quiet and summary times are editable. Equal quiet start/end disables quiet hours.
Quiet hours and snooze delay event eligibility; manual list actions remain
available. Pending event details remain hidden until eligible; the page shows a
waiting count during quiet hours or snooze. This is in-app availability, not a
promise of device notification delivery.

The running web app checks summaries every minute and when Alerts opens. A missed
summary is generated once at an eligible later check, using its actual generation
time. No collection or delivery is claimed while the app is closed. Price
downloads remain manual. `pricewatch.jobs.enabled=false` disables the scheduled
summary check, not the page check.

Alerts and Reminders pages refresh every minute when visible. Refresh is skipped
after form edits, while a form has focus, or while a disclosure is open. Forms
work without JavaScript; reopening the page then refreshes its state.

## Planned budgets

Weekly and Monthly label a limit for the **current My list estimate**. There is
no purchase accumulator, date rollover, or actual-spending calculation. Checked
items stay included, as in J04.

The estimate uses each item's saved retailer choice. Automatic lowest applies
only when that mode is selected. If an explicit retailer loses its listing or
price, retain the choice and report an unknown amount; do not choose a cheaper
retailer automatically.

The budget has four states: no limit, known subtotal already over the limit,
incomplete total, or within the limit. An incomplete list can still be definitely
over budget when its known subtotal alone exceeds the limit. Otherwise it cannot
be confirmed within budget. Category totals use English level-2 source labels,
fall back to Uncategorised, and include only known line amounts. They reconcile
with the known subtotal. Limits use the same exact decimal range as prices.

## Replenishment reminders

Choose a product, a whole quantity from 1–999, an interval of at least one day,
and an optional retailer. Initial due is creation time plus the interval. The
app does not infer household use from price data.

Clear means handled: next due becomes action time plus the interval. Snooze
moves the later of the current effective due and action time forward 24 hours,
without changing the interval. Due indication follows the shared quiet-hours
policy. Manual clear, snooze and add-to-list actions remain available.

Adding delegates to `ShoppingService.add`. The product and quantity are preserved;
the existing retailer-conflict and combined-quantity rules still apply. A failed
add must not partially change the list. A new explicit retailer selection needs
a current known price; an existing item can retain that same selection if the
price later becomes unknown. The reminder retains its retailer and does not
fall back automatically. Adding does not clear the reminder, start tracking or record a purchase.
A product absent from the accepted catalogue can retain its reminder but cannot
be added to My list until available again.

## Web interface

These are normal HTML pages and POST forms, not a separate JSON API.

| Request | Purpose / main fields |
|---|---|
| `GET /saved?code=…` | Wishlist/history; optional product to save or track |
| `POST /saved/save`, `/saved/remove` | Wishlist action: `code` |
| `POST /tracking/start` | `code`, `retailerCode` |
| `POST /tracking/stop`, `/tracking/history/delete` | `trackingId` |
| `GET /alerts` | Rules, timing settings and recent events |
| `POST /alerts/target` | `trackingId`, optional `targetPrice` |
| `POST /alerts/drop` | `trackingId`, `enabled` |
| `POST /alerts/settings` | `quietStart`, `quietEnd`, `summaryTime` |
| `POST /alerts/events/{eventId}/{action}` | `action` is `read` or `snooze` |
| `GET /budget` | Current estimate, limit result and category totals |
| `POST /budget` | `period` (`WEEKLY`/`MONTHLY`), `limit` |
| `GET /reminders?code=…` | Saved reminders; optional product to configure |
| `POST /reminders/create` | `code`, `quantity`, `intervalDays`, optional `retailerCode` |
| `POST /reminders/{id}/{action}` | `action` is `clear`, `snooze`, `add`, or `delete` |

`/planned/saved` redirects to `/saved`; `/planned/updates` redirects to `/alerts`.
Ask remains planned. The shared navigation, import status and local request guard
apply to the new pages. Invalid form input must produce a useful error rather
than a partial write.

## Verification and later work

See the [UML index](diagrams/README.md) for the current extension and historical
baseline. Keep editable sources and use official PlantUML exports. A diagram or
successful compile alone is not acceptance evidence.

Use the [setup guide](SETUP.md#optional-browser-regression-checks) for browser and
database restart/upgrade checks. The current [test catalogue](Test/test-case-catalog.md)
links focused cases; [history](Test/history.md) records actual commands, versions,
outcomes and limits. Earlier J01–J04 results do not verify this extension.

J09 chat, J10 separate-copy transfer, J11 offline reopening and full J12
translation/accessibility/personal-data work remain later jobs. Existing
conditional scope rows keep their evidence conditions. No new cloud or device
notification channel is included.
