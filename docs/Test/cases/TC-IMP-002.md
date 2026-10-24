# TC-IMP-002 — Rejected import keeps the last accepted catalogue

| Field | Details |
|---|---|
| Test class | `CatalogueImporterTest` |
| Test methods | `invalidJsonKeepsLastAcceptedCatalogue()`, `emptyArrayKeepsLastAcceptedCatalogue()`, `productWithoutThreeLabelsIsRejected()`, `duplicateCodeIsRejected()`, `unparseablePriceStaysUnknownNotZero()`, `missingPriceStringStaysUnknown()` |
| Type | Full application context test with constructed invalid input |
| Input | Inline JSON strings passed to `importJson(byte[], sourceUrl)` |

## Purpose

Verify that validation happens before replacement, and that a bad price never
becomes zero.

## Steps

1. Import the good sample so an accepted catalogue exists.
2. Send each invalid payload in turn.
3. Read the stored catalogue after each attempt.

## Expected result

- Not-JSON and `[]` are rejected; the three sample products remain.
- A product missing any of the three name labels is rejected; the accepted
  catalogue is kept.
- Two products sharing one code are rejected with a message naming the code.
- A price of `"ask"` is not rejected wholesale: it becomes `null` and never
  `BigDecimal.ZERO`. The same applies when the price field is absent.

## Limits

The invalid payloads are constructed test data, kept separate from the saved
source sample. The download-failure path (HTTP error, timeout) is not covered
here.

Source: [`CatalogueImporterTest.java`](../../../src/test/java/com/pricewatch/importing/CatalogueImporterTest.java)
