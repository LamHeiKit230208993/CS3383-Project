package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.shopping.ShoppingService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * J12 R46: feedback and errors produced by Java (flash notices, service errors,
 * page titles) must also follow the LANG cookie, not only template text.
 */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:localized;DB_CLOSE_DELAY=-1",
        "spring.main.web-application-type=servlet", "pricewatch.jobs.enabled=false"})
@AutoConfigureMockMvc
@Transactional
class LocalizedFeedbackTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;
    @Autowired ShoppingService shopping;

    @Test void pageTitlesFollowTheLanguageCookie() throws Exception {
        mvc.perform(get("/budget").cookie(new Cookie("LANG", "zh_TW")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("title", "預算"));
        mvc.perform(get("/history").cookie(new Cookie("LANG", "zh_CN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("title", "价格记录"));
        mvc.perform(get("/my-data").cookie(new Cookie("LANG", "zh_TW")))
                .andExpect(model().attribute("title", "設定"));
        mvc.perform(get("/")).andExpect(model().attribute("title", "Discover"));
    }

    @Test void flashNoticesFollowTheLanguageCookie() throws Exception {
        catalog.importSample();
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "1")
                        .cookie(new Cookie("LANG", "zh_TW")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("notice", "項目已加入你的清單。"));
        mvc.perform(post("/my-data/delete").param("confirm", "nope")
                        .cookie(new Cookie("LANG", "zh_CN")))
                .andExpect(flash().attribute("error", "请输入 DELETE 以确认。未删除任何资料。"));
    }

    @Test void serviceErrorsAreShownInTheActiveLanguage() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 1);
        // A second add with a different retailer is rejected by the service with a keyed error.
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "1")
                        .param("retailerCode", "JASONS")
                        .cookie(new Cookie("LANG", "zh_TW")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        containsString("此產品已在你的清單中但使用不同零售商")));
        mvc.perform(get("/products/NO-SUCH-CODE").cookie(new Cookie("LANG", "zh_CN")))
                .andExpect(status().isNotFound());
    }

    @Test void englishRemainsTheFallbackForTheSameFeedback() throws Exception {
        mvc.perform(post("/my-data/delete").param("confirm", "nope"))
                .andExpect(flash().attribute("error", "Type DELETE to confirm. Nothing was removed."));
        mvc.perform(get("/budget")).andExpect(model().attribute("title", "Budget"));
    }
}
