package com.pricewatch.reminder;

import java.time.Instant;
import java.util.List;

public record ReminderOverview(List<ReminderView> reminders, boolean quietHours, Instant nextAllowedAt) {
    public ReminderOverview {
        reminders = List.copyOf(reminders);
    }
}
