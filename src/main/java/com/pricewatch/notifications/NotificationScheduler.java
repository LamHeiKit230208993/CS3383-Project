package com.pricewatch.notifications;

import com.pricewatch.reminder.ReminderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "pricewatch.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {
    private final ReminderService reminders;
    private final WatchRuleService watchRules;

    public NotificationScheduler(ReminderService reminders, WatchRuleService watchRules) {
        this.reminders = reminders;
        this.watchRules = watchRules;
    }

    @Scheduled(fixedDelay = 60_000)
    public void processDueEvents() {
        reminders.recordDue();
        watchRules.checkDue();
    }
}
