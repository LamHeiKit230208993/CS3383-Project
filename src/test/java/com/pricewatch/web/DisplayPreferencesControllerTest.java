package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:web;DB_CLOSE_DELAY=-1",
        "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Transactional
class DisplayPreferencesControllerTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalogue;

    @Test void englishProductViewsUseOneLocalizedName() throws Exception {
        catalogue.importSample();
        mvc.perform(post("/preferences").param("language", "en"))
                .andExpect(redirectedUrl("/preferences"));
        for (String route : new String[]{"/", "/?q=milk", "/products/P000000124"}) {
            mvc.perform(get(route)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("100 %Fresh Milk 236mL")))
                    .andExpect(content().string(not(containsString("鮮牛奶 236毫升"))))
                    .andExpect(content().string(not(containsString("鲜牛奶 236毫升"))));
        }
    }

    @Test void languageAndContrastAreSavedForNewLocalSessions() throws Exception {
        catalogue.importSample();
        for (var language : new String[][]{
                {"en", "Find a product", "100 %Fresh Milk 236mL", "Milk / Milk drinks"},
                {"zh-Hant", "搜尋商品", "鮮牛奶 236毫升", "牛奶 / 牛奶飲品"},
                {"zh-Hans", "搜索商品", "鲜牛奶 236毫升", "牛奶 / 牛奶饮品"}
        }) {
            mvc.perform(post("/preferences").session(new MockHttpSession())
                            .param("language", language[0]).param("highContrast", "true"))
                    .andExpect(redirectedUrl("/preferences"))
                    .andExpect(flash().attribute("displayFeedback", "preferences.saved"));
            mvc.perform(get("/").session(new MockHttpSession()).param("q", "P000000124")
                            .param("category", "Milk / Milk drinks"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("lang=\"" + language[0] + "\"")))
                    .andExpect(content().string(containsString("class=\"high-contrast\"")))
                    .andExpect(content().string(containsString(language[1])))
                    .andExpect(content().string(containsString(language[2])))
                    .andExpect(content().string(containsString(
                            "value=\"Milk / Milk drinks\" selected=\"selected\">" + language[3])));
        }

        mvc.perform(post("/preferences").param("language", "unsupported"))
                .andExpect(redirectedUrl("/preferences"))
                .andExpect(flash().attribute("displayError", "preferences.unsupportedLanguage"));
        mvc.perform(get("/preferences").session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"zh-Hans\"")))
                .andExpect(content().string(containsString("class=\"high-contrast\"")));

        mvc.perform(post("/preferences").param("language", "en"))
                .andExpect(redirectedUrl("/preferences"));
        mvc.perform(get("/preferences"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lang=\"en\"")))
                .andExpect(content().string(not(containsString("class=\"high-contrast\""))));
    }
}
