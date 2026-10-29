# TC-WEB-001 — Baseline pages and forms

Source: [`HelloControllerTest`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java).
The existing class name is retained. This suite now loads the application,
services, an isolated H2 database, and MockMvc instead of a controller-only
prototype slice. Individual tests roll back their database changes.

Scenarios:

- An empty start shows no loaded prices and no invented product.
- The sample-import form redirects to Data. Search in each source language
  finds the same product code. Comparison shows original offers and prices.
- Ordinary POST forms add, change quantity, check, and remove a list item.
  The rendered amount agrees with the service result without JavaScript.
- Invalid whole quantities do not write records; fractional quantities produce
  a clear bad-input page. Unknown products and planned routes return 404.
- Saved, Updates, and Ask display a planned/unavailable message. Data states
  that startup does not refresh automatically.
- Search input is HTML-escaped. Very large and negative page requests do not
  cause an invalid results slice.
- Replacing an accepted catalogue with the historical sample requires explicit
  confirmation checked by the server, not only a browser prompt.

These are server-rendering and application-integration checks. They do not
replace browser layout/keyboard checks, a real process restart, or live-provider
verification. The earlier three-case UI-prototype suite is retired; its dated
results remain in the catalogue as history.

Refinement checks add ranked Matches/Related tabs and an exact source-category
filter, including URL state. Retailer forms must submit the explicit or automatic
choice and render the selected price on My list. They must retain the saved
choice after refresh makes its price unavailable, show an incomplete subtotal,
and label original offers as not calculated. These describe expected current
behaviour; final run results belong in the dated catalogue record.
- Cross-origin browser writes and non-loopback hostnames are rejected before
  controllers run. Same-origin form submissions still work.

Search-mode regressions load the checked sample and open `Bakery` in Related.
The search form must not carry a hidden Related parameter into a new query.
A new `milk` query returns Best matches. Empty and whitespace-only queries with
a stale `related=true` URL must show both catalogue products with Related mode
disabled. Explicit Related links for nonblank queries still work.
