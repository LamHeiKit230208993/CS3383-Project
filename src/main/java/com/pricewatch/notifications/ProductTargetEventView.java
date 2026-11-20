package com.pricewatch.notifications;

import com.pricewatch.notifications.persistence.ProductTargetRetailerPrice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductTargetEventView(long id, String productCode, BigDecimal target,
                                     Instant retrievedAt, Instant createdAt,
                                     List<ProductTargetRetailerPrice> retailers,
                                     boolean read, boolean available, Instant readAt, Instant snoozedUntil,
                                     Long ruleId, Instant scheduledDueAt) {}
