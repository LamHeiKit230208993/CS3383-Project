# TC-OFFLINE-001 — Read-only offline saved list (J11)

Covers a downloaded, self-contained HTML copy of My list (R32 offline
reopening). The copy is a single file with no external CSS, JavaScript, images
or network calls, so it opens after the app is closed and the device is offline.
Dated execution results are recorded separately in the test history.

Sources: [`OfflineListExporterTest`](../../../src/test/java/com/pricewatch/shopping/OfflineListExporterTest.java),
[`HelloControllerTest`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java).

## Export

`GET /list/offline` returns a `text/html` attachment named
`market-desk-list.html`. The body inlines its CSS and a small script; it has no
`<link>` stylesheet and no `src=` reference, so nothing is fetched from a server.
It lists each product's name, brand and code, quantity, retailer label
(Automatic lowest or a saved retailer), unit price, line total, and checked
state, plus the list summary and the export time in HKT.

Only the chosen My list is exported; no accounts, wishlist or tracking data.

## Saved amounts show their age

The header states the saved time ("Prices saved … HKT") and that amounts may be
out of date. The copy makes no fresh-price or remote-chat claim.

## Interactive tick-off

Each item has a checkbox. Ticking an item strikes its name through and updates a
"X of N done" counter. Ticks are local to the open file and are not saved.
Totals stay as saved; checking an item does not remove its cost, matching the
app's planned-total rule.

## Incomplete totals

When an item has no current price, the copy shows the "Known subtotal" label and
a warning that the total is incomplete, rather than presenting a partial sum as
the full cost.

A downloaded copy is the saved read-only record; the app does not keep a
separate server-side snapshot for this job.
