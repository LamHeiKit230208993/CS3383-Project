package com.pricewatch.catalog.importer;

import com.pricewatch.catalog.LocalizedText;

import java.math.BigDecimal;
import java.util.List;

public record ImportedCatalog(List<ImportedProduct> products, int listingCount, int offerCount) {
    public record ImportedProduct(
            String code,
            LocalizedText brand,
            LocalizedText name,
            LocalizedText category1,
            LocalizedText category2,
            LocalizedText category3,
            List<ImportedListing> listings) {
    }

    public record ImportedListing(String retailerCode, BigDecimal price, List<LocalizedText> offers) {
    }
}
