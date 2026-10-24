# TC-IMP-001 — Accepted import

| Field | Details |
|---|---|
| Test class | `CatalogueImporterTest` |
| Test methods | `sampleImportsTheThreeProducts()`, `sampleKeepsThreeLanguageNamesOnOneCode()`, `differentPacksStaySeparateCodes()`, `offerTextIsKeptWhole()`, `retrievalTimeIsRecordedButSourceDateIsUnknown()`, `repeatedImportDoesNotDuplicateListings()` |
| Type | Full application context test with the bundled sample file |
| Input | `src/main/resources/samples/pricewatch-products.json`, three products |

## Purpose

Verify the J02 rules that apply when the incoming file is valid.

## Steps

1. Import the sample through `CatalogueImporter.importSample()`.
2. Read the stored catalogue.
3. Import the sample a second time and compare the listing count.

## Expected result

- The import is accepted and reports three products.
- One code holds English, Traditional Chinese, and Simplified Chinese names;
  all three names resolve to `P000000124`.
- `P000000124` (236 mL) and `P000000125` (946 mL) stay separate products with
  separate prices.
- The AEON listing for `P000000229` keeps `Buy 3 at $30.00` as offer text, and
  its listed price is HK$11.50 — the multi-buy amount is not divided.
- `importedAt` is set; `sourceDate` is `null`.
- A second import leaves the product and listing counts unchanged.

## Limits

Uses a small constructed sample, not the live daily file. The daily download
itself is exercised by the browser check recorded in the catalog.

Source: [`CatalogueImporterTest.java`](../../../src/test/java/com/pricewatch/importing/CatalogueImporterTest.java)
