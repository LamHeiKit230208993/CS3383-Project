package com.pricewatch.notifications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "pricewatch.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {
    private final AlertService alertService;
    private final Clock clock;

    public NotificationScheduler(AlertService alertService, Clock appClock) {
        this.alertService = alertService;
        this.clock = appClock;
    }

    @Scheduled(fixedDelay = 60_000)
    public void processDueEvents() {
        alertService.processDue(clock.instant());
    }
}
