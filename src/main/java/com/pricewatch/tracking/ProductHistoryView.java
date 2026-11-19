package com.pricewatch.tracking;

import com.pricewatch.catalog.LocalizedText;

import java.util.List;

public record ProductHistoryView(String code, LocalizedText name, LocalizedText brand, boolean active,
                                 List<RetailerHistoryView> periods, int retentionDays, int gapDays) {
    public ProductHistoryView {
        periods = List.copyOf(periods);
    }

    public boolean hasEntries() {
        return periods.stream().anyMatch(RetailerHistoryView::hasEntries);
    }
}
