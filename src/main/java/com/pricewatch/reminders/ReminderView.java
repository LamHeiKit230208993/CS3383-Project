package com.pricewatch.reminders;

import java.time.Instant;

/** Reminder as shown on the reminders page. Due state is computed from the clock. */
public record ReminderView(
        Long id,
        String productCode,
        String productName,
        int quantity,
        int intervalDays,
        int snoozeDays,
        Instant nextDueAt,
        boolean active,
        boolean due) {
}
