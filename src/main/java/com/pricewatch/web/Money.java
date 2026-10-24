package com.pricewatch.web;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public final class Money {

    private Money() {
    }

    public static String hkd(BigDecimal amount, String lang) {
        if (amount == null) {
            return "";
        }
        Locale locale = "zh-Hant".equals(lang) ? Locale.TRADITIONAL_CHINESE
                : "zh-Hans".equals(lang) ? Locale.SIMPLIFIED_CHINESE
                : Locale.UK;
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(Currency.getInstance("HKD"));
        return format.format(amount);
    }
}
