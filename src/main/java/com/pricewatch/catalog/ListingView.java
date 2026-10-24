package com.pricewatch.catalog;

import java.math.BigDecimal;
import java.util.List;

public record ListingView(String retailerCode, BigDecimal price, List<LocalizedText> offers) {
    public ListingView {
        offers = List.copyOf(offers);
    }
}
