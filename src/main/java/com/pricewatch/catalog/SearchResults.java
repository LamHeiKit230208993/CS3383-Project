package com.pricewatch.catalog;

import java.util.List;

public record SearchResults(List<ProductView> matches, List<ProductView> related, List<String> categories) {
    public SearchResults {
        matches = List.copyOf(matches);
        related = List.copyOf(related);
        categories = List.copyOf(categories);
    }
}
