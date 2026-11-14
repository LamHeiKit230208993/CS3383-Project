package com.pricewatch.notifications;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AlertEventView(long id, String type, String message, String productCode,
                             String productName, String retailerCode, BigDecimal price,
                             Instant observedAt, BigDecimal previousPrice, Instant previousObservedAt,
                             Instant createdAt, Instant availableAt, Instant snoozedUntil,
                             boolean read, boolean available, Integer summaryCount, LocalDate summaryDate) {
}
