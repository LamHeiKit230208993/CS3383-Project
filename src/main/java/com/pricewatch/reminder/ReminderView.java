package com.pricewatch.reminder;

import java.time.Instant;

public record ReminderView(
        Long id,
        String productCode,
        String productName,
        String brandName,
        int quantity,
        int intervalDays,
        String selectedRetailerCode,
        boolean currentPriceAvailable,
        boolean productActive,
        Instant nextDueAt,
        Instant effectiveDueAt,
        boolean due) {
}
