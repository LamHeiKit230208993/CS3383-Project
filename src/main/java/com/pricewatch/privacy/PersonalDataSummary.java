package com.pricewatch.privacy;

/** Counts of stored personal records. Catalogue products are deliberately absent. */
public record PersonalDataSummary(long lists, long listItems, long wishlist, long trackingPeriods,
                                 long observations, long alerts, long reminders,
                                 boolean preferences, boolean budget, boolean notificationSettings) {
}
