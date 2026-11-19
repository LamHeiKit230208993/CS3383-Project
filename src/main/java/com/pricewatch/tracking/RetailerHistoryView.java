package com.pricewatch.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RetailerHistoryView(long id, String retailerCode, boolean enabled, Instant startedAt,
                                  Instant stoppedAt, BigDecimal targetPrice, boolean dropEnabled,
                                  List<HistoryEntryView> entries) {
    public RetailerHistoryView {
        entries = List.copyOf(entries);
    }

    public boolean hasEntries() {
        return !entries.isEmpty();
    }
}
