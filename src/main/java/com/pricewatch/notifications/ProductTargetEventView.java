package com.pricewatch.notifications;

import com.pricewatch.notifications.persistence.ProductTargetRetailerPrice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductTargetEventView(long id, String productCode, BigDecimal target,
                                     Instant retrievedAt, Instant createdAt,
                                     List<ProductTargetRetailerPrice> retailers,
                                     boolean read, boolean available, Instant readAt, Instant snoozedUntil,
                                     Long ruleId, Instant scheduledDueAt, Integer cartQuantity,
                                     String cartRetailer, boolean activeReminder, boolean cartAvailable,
                                     WatchPriceKind priceKind) {
    public ProductTargetEventView(long id, String productCode, BigDecimal target,
                                  Instant retrievedAt, Instant createdAt,
                                  List<ProductTargetRetailerPrice> retailers,
                                  boolean read, boolean available, Instant readAt, Instant snoozedUntil,
                                  Long ruleId, Instant scheduledDueAt) {
        this(id, productCode, target, retrievedAt, createdAt, retailers, read, available,
                readAt, snoozedUntil, ruleId, scheduledDueAt, null, null, false, false, WatchPriceKind.TARGET);
    }
}
