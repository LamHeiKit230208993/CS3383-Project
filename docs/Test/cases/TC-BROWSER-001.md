# TC-BROWSER-001 — Repeatable baseline browser checks

Source: [market-desk.cjs](../../../src/test/browser/market-desk.cjs).
Prerequisites and commands: [setup guide](../../SETUP.md#optional-browser-regression-checks).

The script launches the packaged app on an available loopback port. It uses an
in-memory H2 database and local test feed. It never uses the normal saved database
or a running app. The original sample records are combined with explicit synthetic
records for search relevance and decimal boundaries.

Nine scenarios cover:

1. Empty start and sample import.
2. Card-name, price, badge, and padding clicks; selected markers, keyboard selection, accessible price/availability descriptions, and independent offer disclosures at 375/1440 px.
3. Required retailer choice, quantity estimate, saved choice, and deliberate edits.
4. Native card selection and forms with JavaScript disabled.
5. Search normalization, both Chinese source labels, Related results, and category URL state.
6. Exact four-decimal and large-price previews; invalid exponent quantities.
7. A disappeared selected price stays unknown without retailer fallback; its card clearly reports unavailable pricing.
8. Five-route layouts at four widths, unclipped prices, mobile navigation, and keyboard skip link.
9. No JavaScript or console errors in the main browser context.

The script stops its app and feed server on completion or failure. Artifacts go
to ignored `target/browser-checks/`. A missing Java executable was also checked:
the script reports the startup error and exits without waiting on an absent child.

These checks do not verify a live provider response, process-restart persistence,
all accessibility requirements, or all browser engines. Existing Java persistence
and validation tests remain separate checks.
