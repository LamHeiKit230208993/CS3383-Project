# TC-DATA-002 — Source parser

Source: [`CatalogJsonParserTest`](../../../src/test/java/com/pricewatch/catalog/CatalogJsonParserTest.java).

Read the unchanged historical sample from the app classpath. Expect 2 products,
8 retailer listings, 4 offer entries, the two Chinese milk names, and the original
multi-buy text. Do not change the source sample to make a test pass.

Constructed inputs check a blank price, offers without a known price, conflicting
duplicate products, and decimal values requiring more than four fractional
places. Expected: unknown prices remain null and invalid input is rejected.
These are parser checks; accepted-database replacement is checked separately.

Further constructed cases reject trailing JSON documents, negative/fractional
numeric product codes, and conflicting numeric/text identifiers after
normalization. A nonnegative integral numeric code becomes its exact decimal
string. Source string codes retain their original string identity. This rule
supports the numeric `173520` observed in the 24 October live response.
