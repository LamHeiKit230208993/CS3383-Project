package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.LocalizedText;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** J12 R46/R47: three UI languages, the LANG switch, and the contrast toggle. */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:locale;DB_CLOSE_DELAY=-1",
        "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Transactional
class LocaleUiTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;

    @Test void englishIsTheDefaultAndWordingIsUnchanged() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"en\"")))
                .andExpect(content().string(containsString("Find a product")))
                .andExpect(content().string(not(containsString("尋找產品"))));
    }

    @Test void traditionalChineseCookieRendersTraditionalWording() throws Exception {
        mvc.perform(get("/").cookie(new Cookie("LANG", "zh_TW"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"zh-Hant\"")))
                .andExpect(content().string(containsString("尋找產品")))
                .andExpect(content().string(containsString("我的清單")));
    }

    @Test void simplifiedChineseCookieRendersSimplifiedWording() throws Exception {
        mvc.perform(get("/").cookie(new Cookie("LANG", "zh_CN"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"zh-Hans\"")))
                .andExpect(content().string(containsString("寻找产品")))
                .andExpect(content().string(not(containsString("尋找產品"))));
    }

    @Test void searchWorksInEveryLanguageAfterOneImport() throws Exception {
        catalog.importSample();
        for (String query : new String[]{"100 %Fresh Milk", "鮮牛奶", "鲜牛奶"}) {
            mvc.perform(get("/").param("q", query)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("P000000124")));
        }
        mvc.perform(get("/").param("q", "鮮牛奶").cookie(new Cookie("LANG", "zh_TW")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("鮮牛奶")));
    }

    @Test void languageSwitchSetsTheCookieAndRedirectsLocally() throws Exception {
        mvc.perform(get("/lang").param("code", "zh_TW").param("back", "/list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/list"))
                .andExpect(cookie().value("LANG", "zh_TW"));
        mvc.perform(get("/lang").param("code", "xx").param("back", "/")).andExpect(cookie().value("LANG", "en"));
        mvc.perform(get("/lang").param("code", "zh_CN").param("back", "//evil.example"))
                .andExpect(redirectedUrl("/"));
    }

    @Test void contrastToggleMarksTheDocumentAndKeepsMeaningInText() throws Exception {
        mvc.perform(get("/my-data").cookie(new Cookie("CONTRAST", "high"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("data-contrast=\"high\"")))
                .andExpect(content().string(containsString("aria-pressed=\"true\"")));
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data-contrast=\"high\""))));
        mvc.perform(get("/contrast").param("mode", "high").param("back", "/list"))
                .andExpect(redirectedUrl("/list"))
                .andExpect(cookie().value("CONTRAST", "high"));
    }

    @Test void englishHidesTheChineseProductNameButChineseShowsTheOtherScript() throws Exception {
        catalog.importSample();
        var product = catalog.find("P000000124");
        String traditional = product.name().zhHant();
        String simplified = product.name().zhHans();
        // English: the product name is not repeated in Chinese on any screen.
        for (String page : new String[]{"/", "/products/P000000124"}) {
            mvc.perform(get(page)).andExpect(status().isOk())
                    .andExpect(content().string(not(containsString(traditional))))
                    .andExpect(content().string(not(containsString(simplified))));
        }
        // A Chinese UI shows the product name in its own script.
        mvc.perform(get("/").cookie(new Cookie("LANG", "zh_CN"))).andExpect(status().isOk())
                .andExpect(content().string(containsString(simplified)));
        mvc.perform(get("/").cookie(new Cookie("LANG", "zh_TW"))).andExpect(status().isOk())
                .andExpect(content().string(containsString(traditional)));
    }

    @Test void productLabelFollowsTheUiLanguageAndFallsBackToEnglish() {
        LocalizedText text = new LocalizedText("Fresh Milk", "鮮牛奶", "鲜牛奶");
        assertEquals("Fresh Milk", ProductLabels.label(text, Locale.ENGLISH));
        assertEquals("鮮牛奶", ProductLabels.label(text, Locale.forLanguageTag("zh-TW")));
        assertEquals("鲜牛奶", ProductLabels.label(text, Locale.forLanguageTag("zh-CN")));
        assertEquals("Fresh Milk",
                ProductLabels.label(new LocalizedText("Fresh Milk", "", null), Locale.forLanguageTag("zh-TW")));
    }
}
