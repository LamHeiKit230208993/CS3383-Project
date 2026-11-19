package com.pricewatch.tracking;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row in a retailer history. A gap row records that no accepted import
 * observed a price change; it is not a generated daily value.
 */
public record HistoryEntryView(boolean gap, BigDecimal price, Instant observedAt, long gapDays) {
    public static HistoryEntryView observation(BigDecimal price, Instant observedAt) {
        return new HistoryEntryView(false, price, observedAt, 0);
    }

    public static HistoryEntryView gap(long days) {
        return new HistoryEntryView(true, null, null, days);
    }
}
