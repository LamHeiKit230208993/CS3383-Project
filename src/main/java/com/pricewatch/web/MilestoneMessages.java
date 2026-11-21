package com.pricewatch.web;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

final class MilestoneMessages {
    private MilestoneMessages() {}

    static String text(MessageSource messages, String key, Object... arguments) {
        return messages.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }

    static String inputError(MessageSource messages, IllegalArgumentException exception) {
        String error = exception.getMessage();
        String key = switch (error) {
            case "A product code is required.", "A product is required." -> "milestone.error.product";
            case "That product is not in the accepted catalogue." -> "milestone.error.productMissing";
            case "That product is no longer in the current catalogue." -> "milestone.error.productInactive";
            case "Choose a retailer to track." -> "milestone.error.trackRetailer";
            case "Choose a retailer currently listed for this product." -> "milestone.error.retailerMissing";
            case "Tracking period was not found." -> "milestone.error.trackingMissing";
            case "Use Stop all retailers to end product-wide tracking." -> "milestone.error.stopAll";
            case "Start a new tracking period before changing its alerts." -> "milestone.error.trackingInactive";
            case "Use a price from 0 to 999999999999999.9999." -> "milestone.error.price";
            case "Alert was not found." -> "milestone.error.alertMissing";
            case "Choose a product from the current catalogue." -> "milestone.error.chooseProduct";
            case "Quantity must be a whole number from 1 to 999." -> "milestone.error.quantity";
            case "Choose an interval of at least 1 day." -> "milestone.error.interval";
            case "Choose a listed retailer for this product, or use Automatic lowest." -> "milestone.error.chooseRetailer";
            case "That interval is outside the supported date range." -> "milestone.error.intervalRange";
            case "That reminder no longer exists." -> "milestone.error.reminderMissing";
            case "Choose a retailer with a current price for this product." -> "milestone.error.noPrice";
            case "This product is already in your list with another retailer; change retailer in My list." ->
                    "milestone.error.listRetailerConflict";
            case "Choose a weekly or monthly budget period." -> "milestone.error.budgetPeriod";
            case "Enter a nonnegative limit with up to 15 digits before and 4 digits after the decimal point." ->
                    "milestone.error.budgetLimit";
            case null, default -> "milestone.error.input";
        };
        return messages.getMessage(key, null, error == null ? "" : error, LocaleContextHolder.getLocale());
    }
}
