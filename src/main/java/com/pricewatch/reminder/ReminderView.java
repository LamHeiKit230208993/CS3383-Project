package com.pricewatch.reminder;

import java.time.Instant;
import java.util.List;

public record ReminderView(
        Long id,
        String productCode,
        String productName,
        String brandName,
        int quantity,
        int intervalDays,
        String selectedRetailerCode,
        boolean currentPriceAvailable,
        boolean addAllowed,
        List<RetailerChoiceView> availableRetailers,
        boolean productActive,
        Instant nextDueAt,
        Instant effectiveDueAt,
        boolean due) {
    public ReminderView {
        availableRetailers = List.copyOf(availableRetailers);
    }
}
