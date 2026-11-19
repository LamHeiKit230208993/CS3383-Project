package com.pricewatch.preferences;

import com.pricewatch.catalog.LocalizedText;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:i18n;DB_CLOSE_DELAY=-1",
        "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Transactional
class LocalizationTest {
    private static final ResourceBundle.Control NO_FALLBACK = new ResourceBundle.Control() {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return null;
        }
    };

    @Autowired MockMvc mvc;

    @Test
    void englishIsTheDefaultLanguage() throws Exception {
        mvc.perform(get("/budget"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"en\"")))
                .andExpect(content().string(containsString("Your planned limit")))
                .andExpect(content().string(containsString("PLAN YOUR SHOP")));
    }

    @Test
    void savedTraditionalChineseAppliesToEveryPage() throws Exception {
        mvc.perform(post("/preferences").param("locale", "zh-HK").param("contrast", "normal"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/preferences"));

        mvc.perform(get("/budget"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"zh-Hant\"")))
                .andExpect(content().string(containsString("預算")))
                .andExpect(content().string(containsString("你的計劃上限")));
    }

    @Test
    void savedSimplifiedChineseAppliesToEveryPage() throws Exception {
        mvc.perform(post("/preferences").param("locale", "zh-CN").param("contrast", "high"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"zh-Hans\"")))
                .andExpect(content().string(containsString("data-contrast=\"high\"")))
                .andExpect(content().string(containsString("我的清单")));
    }

    @Test
    void invalidPreferenceDoesNotChangeTheSavedLanguage() throws Exception {
        mvc.perform(post("/preferences").param("locale", "zh-HK").param("contrast", "normal"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/preferences").param("locale", "fr").param("contrast", "dim"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/preferences"));

        mvc.perform(get("/budget"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"zh-Hant\"")))
                .andExpect(content().string(containsString("你的計劃上限")));
    }

    @Test
    void messageBundlesHaveTheSameKeys() {
        Set<String> english = keys("messages");
        assertEquals(english, keys("messages_zh_HK"));
        assertEquals(english, keys("messages_zh_CN"));
    }

    @Test
    void productLabelsFallBackInTheReviewedOrder() {
        var label = new LocalizedText("Milk", null, "牛奶");
        assertEquals("Milk", label.textFor(Locale.ENGLISH));
        assertEquals("Milk", label.textFor(Locale.forLanguageTag("zh-HK")));
        assertEquals("牛奶", label.textFor(Locale.forLanguageTag("zh-CN")));
    }

    private static Set<String> keys(String bundle) {
        try {
            return new TreeSet<>(ResourceBundle.getBundle(bundle, Locale.ROOT, NO_FALLBACK).keySet());
        } catch (MissingResourceException exception) {
            throw new AssertionError("Missing message bundle " + bundle, exception);
        }
    }
}
