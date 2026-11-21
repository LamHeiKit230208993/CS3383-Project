package com.pricewatch.reminder;

import java.time.Instant;

public record ReminderOccurrenceView(long id, long reminderId, String productCode, int quantity,
                                     String retailerCode, Instant dueAt, Instant createdAt,
                                     boolean read, boolean available, boolean active,
                                     boolean currentPriceAvailable) {
}
