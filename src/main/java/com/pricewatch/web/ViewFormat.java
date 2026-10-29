package com.pricewatch.web;

import com.pricewatch.catalog.ProductView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

@Component
public class ViewFormat {
    private static final String NOT_LISTED = "Not listed";

    private final DateTimeFormatter timeFormat;

    public ViewFormat(ZoneId pricewatchZone) {
        this.timeFormat = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm 'HKT'", Locale.ENGLISH)
                .withZone(pricewatchZone);
    }

    public boolean hasListingPrice(ProductView product, String retailerCode) {
        return product.listings().stream().anyMatch(listing ->
                listing.retailerCode().equals(retailerCode) && listing.price() != null);
    }

    public String money(BigDecimal value) {
        if (value == null) return NOT_LISTED;
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-HK"));
        format.setCurrency(Currency.getInstance("HKD"));
        format.setMaximumFractionDigits(Math.max(2, value.stripTrailingZeros().scale()));
        return format.format(value);
    }

    public String time(Instant value) {
        return timeFormat.format(value);
    }
}
