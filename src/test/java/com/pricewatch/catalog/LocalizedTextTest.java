package com.pricewatch.catalog;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizedTextTest {
    @Test
    void usesEnglishThenAnotherTranslationWhenTheRequestedLabelIsBlank() {
        var traditional = Locale.forLanguageTag("zh-Hant");
        var simplified = Locale.forLanguageTag("zh-Hans");

        assertEquals("milk", new LocalizedText("milk", " ", "牛奶").forLocale(traditional));
        assertEquals("鮮奶", new LocalizedText(null, "鮮奶", null).forLocale(simplified));
        assertEquals("鲜奶", new LocalizedText(null, " ", "鲜奶").forLocale(Locale.ENGLISH));
        assertEquals("", new LocalizedText(null, " ", null).forLocale(Locale.ENGLISH));
    }
}
