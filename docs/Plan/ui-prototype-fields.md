# UI prototype — shared field mapping

The prototype uses the logical fields from
[features.md §6](features.md#6-shared-input-and-result-fields). These are sample
values in templates, not storage or API contracts.

| Operation | Prototype location | Fields and `data-*` attributes |
|---|---|---|
| Search | `search.html` | `data-name` (product name, brand, Chinese sample names), `data-category`; result cards show product code context on the compare page |
| Compare | `compare.html` | `product.code`, `product.name`, `product.pack`, `listing.retailer`, `listing.code`, `listing.price`, `listing.offer`, `listing.state` |
| Add to list | `compare.html` dialog, `list.html` | `data-quantity`, `data-price`; quantity input `#item-quantity` |
| Track | `saved.html` | Separate wishlist and tracking cards; tracking links to history |
| Explain | `assistant.html` | Sample question, checked comparison rows, source note, and clarification note |

## Naming rules used in the prototype

- Product identity includes product code, brand, name, and pack.
- A retailer listing belongs to one product and one retailer.
- Missing prices display as `—`, never as zero.
- Offer text is preserved as free text.
- Source date and retrieval date are shown separately.
- The list total is labelled as an estimate, not actual spending.
- A wishlist item is not a tracking record and not a purchase.

## Intended later mapping

The same names should carry into J01–J04 and later jobs: product code, stored
language labels, pack, retailer listing, original offer text, source date,
retrieval time, quantity, and saved-list state. The prototype does not define
routes or storage for those jobs.
