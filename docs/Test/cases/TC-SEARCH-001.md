# TC-SEARCH-001 — Search relevance and category filtering

Source: [`SearchRelevanceTest`](../../../src/test/java/com/pricewatch/catalog/SearchRelevanceTest.java).

Constructed-data scenarios check:

- Product-name hits rank before brand and narrower category hits. Broad level-1
  category matches are returned in Related, separately from Matches.
- Every query token must match. NFKC normalization, lowercasing, and collapsed
  whitespace work across stored labels in all three languages. This is not fuzzy
  spelling correction, translation, or product-code merging.
- The category filter matches the exact normalized English level-2 source label.
  Available categories come from the accepted catalogue.

The service returns `SearchResults(matches, related, categories)`. The older
`search(query)` combines Matches followed by Related for compatible callers.
Browser checks separately verify tabs, filter URL state, and navigation.

A mixed query such as `instant food` keeps a direct name match in Matches even
when the other token is found only in the broad source category. Only entirely
broad-category matches belong in Related.
