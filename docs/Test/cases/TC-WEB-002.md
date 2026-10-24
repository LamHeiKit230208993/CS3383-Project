# TC-WEB-002 — Compare page shows ties, offers, and unknown prices

| Field | Details |
|---|---|
| Test class | `PageControllerTest` |
| Test methods | `compareShowsTiesAndRetrievedTime()`, `compareOfUnknownCodeRedirectsHome()`, `missingPriceIsLabelledAndNotZero()` |
| Type | Full context test rendering the real Thymeleaf template |
| Request | `GET /compare/P000000229`, `GET /compare/NOPE`, `GET /compare/P-UNKNOWN-PRICE` |

## Purpose

Verify the J03 presentation rules on the real page: ties are stated, the
original offer text is shown, the retrieval time is visible, and a missing
price is labelled instead of becoming zero.

## Steps

1. Open the comparison for the four-retailer tied bread.
2. Request an unknown product code.
3. Publish a catalogue where the only listing has a `null` price, then open it.

## Expected result

- The bread page shows `Tied prices`, the text `Buy 3 at $30.00`, and an
  HK$11.50 amount.
- The unknown code redirects instead of rendering a page.
- The unpriced product renders `No price listed` and `No listed price`, and
  the page never contains `HK$0.00`.

## Limits

The unpriced catalogue is constructed for the test. The checked daily file had
no missing prices.

Source: [`PageControllerTest.java`](../../../src/test/java/com/pricewatch/web/PageControllerTest.java)
