package com.pricewatch.catalog;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public record LocalizedText(String en, String zhHant, String zhHans) {
    public String textFor(Locale locale) {
        return valueFor(locale).text();
    }

    public String text() {
        return textFor(LocaleContextHolder.getLocale());
    }

    public String languageFor(Locale locale) {
        return valueFor(locale).language();
    }

    private Label valueFor(Locale locale) {
        boolean traditional = locale != null && "zh".equals(locale.getLanguage())
                && "HK".equalsIgnoreCase(locale.getCountry());
        boolean simplified = locale != null && "zh".equals(locale.getLanguage()) && !traditional;
        if (traditional) {
            return first(new Label(zhHant, "zh-Hant"), new Label(en, "en"), new Label(zhHans, "zh-Hans"));
        }
        if (simplified) {
            return first(new Label(zhHans, "zh-Hans"), new Label(en, "en"), new Label(zhHant, "zh-Hant"));
        }
        return first(new Label(en, "en"), new Label(zhHant, "zh-Hant"), new Label(zhHans, "zh-Hans"));
    }

    private static Label first(Label... candidates) {
        for (Label candidate : candidates) {
            if (candidate.text() != null && !candidate.text().isBlank()) return candidate;
        }
        return new Label("", "en");
    }

    public static String label(String en, String zhHant, String zhHans) {
        return new LocalizedText(en, zhHant, zhHans).text();
    }

    private record Label(String text, String language) {
    }
}
