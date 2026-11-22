package com.pricewatch.reminder;

import java.util.List;

public record ReminderOverview(List<ReminderView> reminders) {
    public ReminderOverview {
        reminders = List.copyOf(reminders);
    }
}
