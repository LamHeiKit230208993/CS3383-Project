package com.pricewatch.alerts;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/** Shared in-app notification policy for alerts, summaries, and reminders (D05). */
public final class NotificationPolicy {
    public static final LocalTime QUIET_START = LocalTime.of(22, 0);
    public static final LocalTime QUIET_END = LocalTime.of(7, 0);
    public static final LocalTime SUMMARY_TIME = LocalTime.of(8, 0);

    private NotificationPolicy() {
    }

    /** True during 22:00–07:00 Hong Kong time. */
    public static boolean isQuietHours(Clock clock) {
        LocalTime now = LocalTime.now(clock);
        return !now.isBefore(QUIET_START) || now.isBefore(QUIET_END);
    }

    /** The most recent 08:00 boundary at or before now. */
    public static ZonedDateTime latestSummaryTime(Clock clock) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime summary = now.with(SUMMARY_TIME);
        return summary.isAfter(now) ? summary.minusDays(1) : summary;
    }
}
