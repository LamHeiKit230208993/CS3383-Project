package com.pricewatch.web;

import com.pricewatch.catalog.LocalizedText;

import java.util.Locale;

/** J12: product label in the active UI language, falling back to English. */
public final class ProductLabels {
    private ProductLabels() {
    }

    public static String label(LocalizedText text, Locale locale) {
        if (text == null) return "";
        String lang = LocaleController.htmlLang(locale);
        if ("zh-Hant".equals(lang) && !blank(text.zhHant())) return text.zhHant();
        if ("zh-Hans".equals(lang) && !blank(text.zhHans())) return text.zhHans();
        return text.en();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
