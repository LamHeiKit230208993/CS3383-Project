package com.pricewatch.personal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record PersonalDataExport(
        int version,
        List<ShoppingList> lists,
        List<WishlistEntry> wishlist,
        List<String> allRetailerTrackingProducts,
        List<TrackedRetailer> tracking,
        List<Observation> observations,
        List<Alert> alerts,
        List<Reminder> reminders,
        List<Budget> budgets,
        Preferences preferences,
        List<Chat> chats,
        List<PriceTarget> priceTargets,
        List<ProductPriceAlert> productPriceAlerts,
        List<TimeAlert> timeAlerts,
        List<WatchRule> watchRules
) {
    public record ShoppingList(long id, String name, List<ListEntry> entries) {}

    public record ListEntry(String productCode, String displayName, int quantity,
                            boolean checked, String retailerCode) {}

    public record WishlistEntry(String productCode, Instant savedAt) {}

    public record TrackedRetailer(long id, String productCode, String retailerCode,
                                  boolean enabled, Instant startedAt, Instant stoppedAt,
                                  BigDecimal targetPrice, boolean targetArmed, boolean dropEnabled, boolean alertsEnabled) {}

    public record Observation(long id, long trackingId, BigDecimal price, Instant retrievedAt) {}

    public record Alert(long id, Long trackingId, Long observationId, String type,
                        BigDecimal price, Instant observedAt, BigDecimal previousPrice, Instant previousObservedAt,
                        Instant createdAt, Instant availableAt, Instant readAt, Instant snoozedUntil,
                        LocalDate summaryDate, Integer summaryCount, Instant summarizedAt) {}

    public record Reminder(long id, String productCode, int quantity, int intervalDays,
                           String retailerCode, Instant nextDueAt, Instant snoozedUntil, boolean enabled) {}

    public record PriceTarget(String productCode, BigDecimal target, boolean armed, boolean enabled) {}

    public record WatchRule(long id, String productCode, BigDecimal target, Integer intervalDays,
                            Instant nextDueAt, boolean armed, boolean waitingForLive, boolean enabled,
                            String repeat, LocalDateTime startAt, boolean completed) {}

    public record ProductPriceAlert(long id, String productCode, BigDecimal target,
                                    Instant retrievedAt, Instant createdAt, List<RetailerPrice> retailers,
                                    Instant readAt, Instant snoozedUntil, Long ruleId, Instant scheduledDueAt) {}

    public record RetailerPrice(String retailerCode, BigDecimal price) {}

    public record TimeAlert(long id, long reminderId, String productCode, int quantity, String retailerCode,
                            Instant dueAt, Instant createdAt, Instant readAt, Instant snoozedUntil) {}

    public record Budget(String period, BigDecimal limitAmount) {}

    public record Preferences(Display display, Notifications notifications) {}

    public record Display(String language, boolean highContrast) {}

    public record Notifications(LocalTime quietStart, LocalTime quietEnd, LocalTime summaryTime) {}

    public record Chat(String id, String title, String productCode, String requestedRetailer,
                       List<Choice> candidates, List<Message> messages) {}

    public record Choice(String productCode, String label) {}

    public record Message(boolean shopper, String text, List<PriceCard> priceCards,
                          List<ShoppingCard> shoppingCards, List<ChatAction> actions) {}

    public record ShoppingCard(String kind, String title, String detail, Long listId, List<ShoppingRow> rows) {}

    public record ShoppingRow(String label, String detail, String productCode, Long listId) {}

    public record ChatAction(String state, ShoppingCard preview) {}

    public record PriceCard(String productCode, String productLabel, List<PriceRow> rows, Instant retrievedAt) {}

    public record PriceRow(String retailer, BigDecimal price, boolean lowest) {}
}
