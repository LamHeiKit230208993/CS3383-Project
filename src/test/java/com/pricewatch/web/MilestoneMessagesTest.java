package com.pricewatch.web;

import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MilestoneMessagesTest {
    @ParameterizedTest
    @CsvSource({
            "'A product code is required.', product",
            "'A product is required.', product",
            "'That product is not in the accepted catalogue.', productMissing",
            "'That product is no longer in the current catalogue.', productInactive",
            "'Choose a retailer to track.', trackRetailer",
            "'Choose a retailer currently listed for this product.', retailerMissing",
            "'Tracking period was not found.', trackingMissing",
            "'Use Stop all retailers to end product-wide tracking.', stopAll",
            "'Start a new tracking period before changing its alerts.', trackingInactive",
            "'Use a price from 0 to 999999999999999.9999.', price",
            "'Alert was not found.', alertMissing",
            "'Choose a product from the current catalogue.', chooseProduct",
            "'Quantity must be a whole number from 1 to 999.', quantity",
            "'Choose an interval of at least 1 day.', interval",
            "'Choose a listed retailer for this product, or use Automatic lowest.', chooseRetailer",
            "'That interval is outside the supported date range.', intervalRange",
            "'That reminder no longer exists.', reminderMissing",
            "'Choose a retailer with a current price for this product.', noPrice",
            "'This product is already in your list with another retailer; change retailer in My list.', listRetailerConflict",
            "'Choose a weekly or monthly budget period.', budgetPeriod",
            "'Enter a nonnegative limit with up to 15 digits before and 4 digits after the decimal point.', budgetLimit",
            "'Unexpected validation detail', input",
            ", input"
    })
    void mapsValidationMessagesToTheVisibleTranslation(String detail, String messageKey) {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("shopping-messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        var locale = Locale.forLanguageTag("zh-Hant");
        try {
            LocaleContextHolder.setLocale(locale);
            assertEquals(messages.getMessage("milestone.error." + messageKey, null, locale),
                    MilestoneMessages.inputError(messages, new IllegalArgumentException(detail)));
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
