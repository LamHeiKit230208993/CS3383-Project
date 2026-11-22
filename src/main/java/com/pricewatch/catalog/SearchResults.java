package com.pricewatch.catalog;

import java.util.List;
import java.util.Map;

public record SearchResults(List<ProductView> matches, List<ProductView> related, List<String> categories,
                            Map<String, LocalizedText> categoryLabels) {
    public SearchResults {
        matches = List.copyOf(matches);
        related = List.copyOf(related);
        categories = List.copyOf(categories);
        categoryLabels = Map.copyOf(categoryLabels);
    }

    public SearchResults(List<ProductView> matches, List<ProductView> related, List<String> categories) {
        this(matches, related, categories, Map.of());
    }
}
