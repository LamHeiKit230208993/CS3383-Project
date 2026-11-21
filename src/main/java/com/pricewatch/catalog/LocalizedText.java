package com.pricewatch.catalog;

import java.util.Locale;

public record LocalizedText(String en, String zhHant, String zhHans) {
    public String forLocale(Locale locale) {
        String preferred = switch (locale.toLanguageTag()) {
            case "zh-Hant" -> zhHant;
            case "zh-Hans" -> zhHans;
            default -> en;
        };
        if (hasText(preferred)) return preferred;
        if (hasText(en)) return en;
        if (hasText(zhHant)) return zhHant;
        return hasText(zhHans) ? zhHans : "";
    }

    private static boolean hasText(String label) {
        return label != null && !label.isBlank();
    }
}
