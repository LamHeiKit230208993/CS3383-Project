# TC-CMP-001 — Comparison never lets a missing price win

| Field | Details |
|---|---|
| Test class | `ComparisonTest` |
| Test methods | `lowestPriceWins()`, `equalPricesAreTies()`, `missingPriceNeverWins()`, `allPricesMissingHasNoWinner()` |
| Type | Plain unit test over an in-memory `Product` |
| Input | Constructed listings with known and `null` prices |

## Purpose

Verify the J03 comparison rules: one cheapest retailer, ties kept, unknown
prices excluded.

## Steps

1. Compare a product with one clearly lower price.
2. Compare a product with two equal lowest prices.
3. Compare a product with one priced and one unpriced listing.
4. Compare a product where every price is `null`.

## Expected result

- The lowest single retailer wins and `isTie()` is `false`.
- Equal lowest prices produce a tie with both retailers in `cheapest`.
- An unpriced retailer appears in `missing`, and never in `cheapest`.
- With no known price, `hasWinner()` is `false` and `cheapest` is empty.

## Limits

Ties and missing prices are constructed cases. The live file contained no
missing prices on the checked day, so the missing-price path is proven by
tests and one constructed browser case, not by real data.

Source: [`ComparisonTest.java`](../../../src/test/java/com/pricewatch/domain/ComparisonTest.java)
