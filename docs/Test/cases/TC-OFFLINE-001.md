# TC-OFFLINE-001 — Read-only offline list snapshot

This case covers the J11 single-file saved-list snapshot. Dated execution
results are recorded separately in the test history.

Sources:
[`OfflineSnapshotServiceTest`](../../../src/test/java/com/pricewatch/catalog/OfflineSnapshotServiceTest.java),
[`ListTransferControllerTest`](../../../src/test/java/com/pricewatch/web/ListTransferControllerTest.java), and
the offline scenario in `src/test/browser/milestone-check.cjs`.

## Generation

Save a list item with quantity, checked state, and a retailer choice. Request
`GET /list/offline` for My list, or `GET /list/offline?listId=...` for another
saved list. Expect an `attachment` named `<list name>.pricewatch.html`,
`text/html` with UTF-8, and one self-contained HTML document.

The document contains:

- the list name and item count;
- product name, brand, product code, quantity, checked state, and retailer;
- the saved unit price and line amount, or `Unknown` when the saved retailer
  has no current price;
- the saved-on timestamp with its Hong Kong time;
- a data-age label that explains the amounts are frozen at that timestamp and
  the file does not update;
- a source price date of `Unknown` and a note that a saved amount is not proof
  of a current price or stock state.

An automatic retailer choice names the retailer whose price supplied the saved
amount. An empty My list still produces a readable snapshot with an empty-state
message. An unknown list id returns 404.

## Self-contained and read-only

The source contains inline CSS only. It has no `<script>`, `<form>`, `<a>`,
`href`, `src`, remote URL, image, iframe, refresh control, chat control, or
editable field. It has no dependency on the running app or a network
connection.

The browser check downloads the file, stops the Spring Boot process, opens the
file through a `file://` URL in an offline browser context, and confirms the
saved products, quantities, retailers, amounts, saved-on label, data-age label,
and unknown source date remain visible. The page also fits 375 px and 1440 px
without horizontal overflow.

## Language and escaping

The snapshot uses the saved UI language for its labels and the existing
source-label fallback for product and brand names, with the label's own `lang`
attribute. Currency amounts stay in Hong Kong dollars. List names and source
labels are HTML-escaped; a name such as `<b>Trip</b>` appears as text and cannot
create markup.

## Limits

The file is a static copy. It never refreshes, and reopening it later does not
change the saved amounts. The displayed age is therefore the elapsed time since
the visible saved-on timestamp, not a clock that updates inside the file. The
snapshot contains one chosen list; it is not a live link or a two-way sync.
