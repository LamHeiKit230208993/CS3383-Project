package com.pricewatch.notifications;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public enum WatchRepeat {
    ONCE, DAILY, WEEKLY, FORTNIGHTLY, MONTHLY;

    private static final ZoneId HONG_KONG = ZoneId.of("Asia/Hong_Kong");

    public static Instant inHongKong(LocalDateTime time) {
        return time.atZone(HONG_KONG).toInstant();
    }

    public static Instant nextHongKongDay(Instant now) {
        return now.atZone(HONG_KONG).toLocalDate().plusDays(1).atStartOfDay(HONG_KONG).toInstant();
    }

    public Instant nextAfter(Instant anchor, Instant now) {
        return nextAfter(LocalDateTime.ofInstant(anchor, HONG_KONG), now);
    }

    public Instant nextAfter(LocalDateTime anchor, Instant now) {
        if (this == ONCE) return null;

        var current = LocalDateTime.ofInstant(now, HONG_KONG);
        long daysPerPeriod = this == WEEKLY ? 7 : this == FORTNIGHTLY ? 14 : 1;
        long elapsed = this == MONTHLY
                ? ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(current))
                : ChronoUnit.DAYS.between(anchor.toLocalDate(), current.toLocalDate()) / daysPerPeriod;
        long period = Math.max(0, elapsed);
        var candidate = atPeriod(anchor, daysPerPeriod, period);
        return inHongKong(candidate.isAfter(current) ? candidate : atPeriod(anchor, daysPerPeriod, period + 1));
    }

    private LocalDateTime atPeriod(LocalDateTime anchor, long daysPerPeriod, long period) {
        return this == MONTHLY ? anchor.plusMonths(period) : anchor.plusDays(daysPerPeriod * period);
    }
}
