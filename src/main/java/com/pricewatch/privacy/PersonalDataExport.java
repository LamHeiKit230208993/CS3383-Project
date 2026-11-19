package com.pricewatch.privacy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Versioned personal-data file. It contains no catalogue product or source records. */
public record PersonalDataExport(String format, int version, Instant exportedAt,
                                 List<ListRecord> lists, List<WishlistRecord> wishlist,
                                 List<TrackingRecord> tracking, List<AlertRecord> alerts,
                                 BudgetRecord budget, List<ReminderRecord> reminders,
                                 PreferencesRecord preferences, NotificationSettingsRecord notificationSettings) {
    public PersonalDataExport {
        lists = List.copyOf(lists);
        wishlist = List.copyOf(wishlist);
        tracking = List.copyOf(tracking);
        alerts = List.copyOf(alerts);
        reminders = List.copyOf(reminders);
    }

    public record ListRecord(String name, List<ItemRecord> items) {
        public ListRecord {
            items = List.copyOf(items);
        }
    }

    public record ItemRecord(String productCode, int quantity, boolean checked, String retailerCode) {
    }

    public record WishlistRecord(String productCode, Instant savedAt) {
    }

    public record TrackingRecord(long id, String productCode, String retailerCode, boolean enabled,
                                 Instant startedAt, Instant stoppedAt, BigDecimal targetPrice,
                                 boolean dropEnabled, List<ObservationRecord> observations) {
        public TrackingRecord {
            observations = List.copyOf(observations);
        }
    }

    public record ObservationRecord(BigDecimal price, Instant observedAt) {
    }

    public record AlertRecord(long id, Long trackingId, String type, BigDecimal price, Instant observedAt,
                              BigDecimal previousPrice, Instant previousObservedAt, Instant createdAt,
                              Instant readAt, Instant snoozedUntil, LocalDate summaryDate, Integer summaryCount) {
    }

    public record BudgetRecord(String period, BigDecimal limit) {
    }

    public record ReminderRecord(long id, String productCode, int quantity, int intervalDays,
                                 String retailerCode, Instant nextDueAt, Instant snoozedUntil) {
    }

    public record PreferencesRecord(String locale, String contrast) {
    }

    public record NotificationSettingsRecord(LocalTime quietStart, LocalTime quietEnd, LocalTime summaryTime) {
    }
}
