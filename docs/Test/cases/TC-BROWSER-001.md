# TC-BROWSER-001 — Repeatable baseline browser checks

Source: [market-desk.cjs](../../../src/test/browser/market-desk.cjs).
Prerequisites and commands: [setup guide](../../SETUP.md#optional-browser-regression-checks).

The script launches the packaged app on an available loopback port. It uses an
in-memory H2 database and local test feed. It never uses the normal saved database
or a running app. The original sample records are combined with explicit synthetic
records for search relevance and decimal boundaries.

The baseline screen checks cover:

1. Empty start and sample import.
2. Card-name, price, badge, and padding clicks; selected markers, keyboard selection, accessible price/availability descriptions, and independent offer disclosures at 375/1440 px.
3. Required retailer choice, quantity estimate and saved choice. Change the
   store inline and use the named quantity stepper by keyboard. Open the item
   by clicking the card outside the form controls and return to Cart. Mark a row
   done, verify its strike-through, undo and repeat. Reload and check the
   persisted quantity, retailer, done state and subtotal. Confirm completed
   rows lock their product link, editing and deletion until reopened.
4. Native card selection and forms with JavaScript disabled.
5. Search normalization, both Chinese source labels, Related results, and category URL state. A new query from Related returns to Best matches; clearing it restores the catalogue, including when opening a stale blank Related URL.
6. Exact four-decimal and large-price previews; invalid exponent quantities.
7. A disappeared selected price stays unknown without retailer fallback; its card clearly reports unavailable pricing.
8. Check desktop and mobile layouts at four widths and keep prices in view.
   Check the shared page background, serif page headings, and sans-serif Cart item names.
   Verify the five mobile tabs, open Settings by keyboard, and reach Price
   data from Settings. Check disabled transitions with reduced motion on
   and use the keyboard skip link.
9. In Wishlist, check that hovering a desktop row action expands its label.
   At phone width, check the category icon and three row actions
   for an exact pack. Check icon labels and 44 px touch targets. Reach Set rules
   by keyboard. Open the product through its name and use Back to Wishlist.
   Batch-add an existing Cart item
   and a new one. Check that the existing retailer choice stays in the Cart.
10. No JavaScript or console errors in the main browser context.
11. Check Discover search by keyboard at 375 px and confirm the sticky
    desktop summary sits beside search. Check the product category artwork,
    the saved budget balance on Discover and Cart, the keyboard disclosure
    for List actions, and Continue browsing. Capture the desktop and mobile
    layouts.
12. In the glass theme, open Recent chats on a short phone viewport, select
    an existing chat, and confirm that the transcript leaves room for the
    composer and the bottom navigation.

The script stops its app and feed server on completion or failure. Artifacts go
to ignored `target/browser-checks/`. A missing Java executable was also checked:
the script reports the startup error and exits without waiting on an absent child.

These checks do not verify a live provider response, process-restart persistence,
all accessibility requirements, or all browser engines. Existing Java persistence
and validation tests remain separate checks.
