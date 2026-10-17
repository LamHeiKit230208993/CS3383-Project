# Price JSON Sample Check

Checked: 17 October 2026. This is one source download, not a completed app feature.

Source: [Consumer Council JSON](https://online-price-watch.consumer.org.hk/opw/opendata/pricewatch.json), linked from the [official dataset page](https://data.gov.hk/en-data/dataset/cc-pricewatch-pricewatch). The official page describes daily updates.

Two unchanged product records from the download are saved in [samples/pricewatch-products.json](samples/pricewatch-products.json). This small file is an example for field checks, not the full catalogue or current prices for users.

## 1. Observed counts

| Check | Result in this download |
|---|---|
| Download size | 2,741,295 bytes, about 2.74 MB |
| Top-level form | JSON array of products |
| Products | 2,736 |
| Retailer price entries | 7,818 |
| Offer entries | 2,482 |
| Retailer codes in price entries | 9 |
| Duplicate product-code groups | 0 |
| Products missing any of the three name labels | 0 |
| Prices that failed decimal parsing or were negative | 0 |

Retailers seen: AEON, DCHFOOD, JASONS, LUNGFUNG, MANNINGS, PARKNSHOP, SASA, WATSONS, WELLCOME. Do not hard-code this count; future downloads can differ.

SHA-256 of the full downloaded file: `614de113e4dd22e2e87bea72875fef1af849b7a31dc0e9f504644619a060b3d0`.

## 2. Fields actually seen

| JSON field | Shape and use |
|---|---|
| `code` | Product code |
| `brand`, `name` | Objects with `en`, `zh-Hant`, and `zh-Hans` labels |
| `cat1Name`, `cat2Name`, `cat3Name` | Three category levels, each with the same language keys |
| `prices` | Array of `supermarketCode` and string `price` values |
| `offers` | Array of `supermarketCode` and offer text under the three language keys |

The product fields seen were `code`, `brand`, `name`, `cat1Name`, `cat2Name`, `cat3Name`, `prices`, and `offers`. There was no separate product source-date field, structured pack field, stock field, or past-price series in these records. Retrieval date must not be called the source price date.

## 3. What this means for search

Product `P000000124` contains these names under one code:

- English: `100 %Fresh Milk 236mL`
- Traditional Chinese: `鮮牛奶 236毫升`
- Simplified Chinese: `鲜牛奶 236毫升`

Store the language labels under that code and search all of them. The display language can change without creating another product. This can support different language names without asking AI to translate or merge products.

This is not proof that every same-looking item with a different code is the same pack. Do not merge codes by similar text alone. One download also cannot prove code stability over time. Spelling errors, short names, mixed-language queries, and words such as “milk” matching milk bread still need search examples and ranking checks.

The saved bread record `P000000229` has four retailer prices and a separate AEON multi-buy offer. Preserve the retailer link and original offer text; do not turn a multi-buy total into a single-pack listed price.

## 4. What this means for history

The endpoint returned one full catalogue. A product/category filter was not tested or established.

The baseline can download the current catalogue, keep it for search, then save older price records only for tracked products. This limits local history rows; it does not establish a smaller network download.

Keeping all-product history is not proven impossible. We have not measured database size, query speed, or growth across real daily files. Selected tracking is a first-baseline choice, not a measured provider limit.

Keep each product/pack/retailer history separate. Do not graph the cheapest item in a changing category as if it were one unchanged product. Available capture dates are not proof of unobserved past prices or future prices.

## 5. Verification limits and next check

Confirmed here: successful download, basic JSON shape, counts, decimal parsing, language-label presence, and the two saved records.

Not checked here: historical downloads, cross-day code stability, full product/pack correctness, source-date semantics, currency and offer rules, a working app import, database storage cost, search quality, AI answers, or notification delivery.

Use the examples in the first import/search jobs. Add invalid and missing-field examples separately and label them as constructed test data. Do not change this source sample to make a test pass.
