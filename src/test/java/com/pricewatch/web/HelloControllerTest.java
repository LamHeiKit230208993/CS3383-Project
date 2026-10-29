package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.shopping.ShoppingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:web;DB_CLOSE_DELAY=-1", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Transactional
class HelloControllerTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;
    @Autowired ShoppingService shopping;

    @Test void emptyStartDoesNotPretendToHavePrices() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk()).andExpect(view().name("market-desk"))
                .andExpect(content().string(containsString("No prices loaded")))
                .andExpect(content().string(containsString("Find a product")))
                .andExpect(content().string(not(containsString("DEMO-MILK"))));
        assertNull(catalog.status());
    }
    @Test void sampleImportAndThreeLanguageSearchRenderSourceIdentity() throws Exception {
        mvc.perform(post("/data/sample")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/data"));
        for (String query : new String[]{"100 %Fresh Milk", "鮮牛奶", "鲜牛奶"}) {
            mvc.perform(get("/").param("q", query)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("P000000124")))
                    .andExpect(content().string(containsString("Historical sample")));
        }
        mvc.perform(get("/products/P000000229")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Buy 3 at $30.00")))
                .andExpect(content().string(containsString("Lowest listed")));
    }
    @Test void formsAddEditCheckAndRemoveWithoutJavaScript() throws Exception {
        catalog.importSample();
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "2"))
                .andExpect(redirectedUrl("/list"));
        mvc.perform(post("/list/items/P000000124/update").param("quantity", "3").param("checked", "true"))
                .andExpect(redirectedUrl("/list"));
        assertEquals(3, shopping.items().getFirst().quantity());
        assertTrue(shopping.items().getFirst().checked());
        mvc.perform(get("/list")).andExpect(status().isOk())
                .andExpect(content().string(containsString("22.50")))
                .andExpect(content().string(containsString("Estimated list cost")));
        mvc.perform(post("/list/items/P000000124/remove")).andExpect(redirectedUrl("/list"));
        assertTrue(shopping.items().isEmpty());
    }
    @Test void validationAndUnknownRoutesDoNotWriteInvalidData() throws Exception {
        catalog.importSample();
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "0"))
                .andExpect(redirectedUrl("/list")).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "1.5"))
                .andExpect(status().isBadRequest()).andExpect(content().string(containsString("Check your input")));
        assertTrue(shopping.items().isEmpty());
        mvc.perform(get("/products/UNKNOWN")).andExpect(status().isNotFound());
        mvc.perform(get("/planned/UNKNOWN")).andExpect(status().isNotFound());
    }
    @Test void replacingAnAcceptedCatalogueRequiresServerSideConfirmation() throws Exception {
        var previous = catalog.importSample();
        mvc.perform(post("/data/sample")).andExpect(redirectedUrl("/data"))
                .andExpect(flash().attributeExists("error"));
        assertEquals(previous.retrievedAt(), catalog.status().retrievedAt());
        mvc.perform(post("/data/sample").param("confirmSample", "true"))
                .andExpect(redirectedUrl("/data")).andExpect(flash().attributeExists("notice"));
    }
    @Test void unrelatedSitesCannotSubmitLocalChanges() throws Exception {
        mvc.perform(post("/data/sample").header("Origin", "https://unrelated.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/data/sample").header("Origin", "null")).andExpect(status().isForbidden());
        mvc.perform(post("/data/sample").header("Sec-Fetch-Site", "cross-site")).andExpect(status().isForbidden());
        mvc.perform(get("/").with(request -> { request.setServerName("unrelated.example"); return request; }))
                .andExpect(status().isForbidden());
        assertNull(catalog.status());
        mvc.perform(post("/data/sample").header("Origin", "http://localhost"))
                .andExpect(redirectedUrl("/data"));
        assertNotNull(catalog.status());
    }
    @Test void chosenRetailerIsSavedAndCanBeChangedExplicitly() throws Exception {
        catalog.importSample();
        mvc.perform(get("/products/P000000124")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose where to buy.")))
                .andExpect(content().string(containsString("name=\"retailerCode\"")))
                .andExpect(content().string(containsString("Offer not included in estimate")));
        mvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "2")
                        .param("retailerCode", "JASONS")).andExpect(redirectedUrl("/list"));
        assertEquals("JASONS", shopping.items().getFirst().selectedRetailerCode());
        assertEquals(0, shopping.items().getFirst().lineTotal().compareTo(new java.math.BigDecimal("25.00")));
        mvc.perform(get("/list")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Your retailer: JASONS")));
        mvc.perform(post("/list/items/P000000124/update").param("quantity", "2")
                        .param("retailerCode", "PARKNSHOP").param("checked", "true"))
                .andExpect(redirectedUrl("/list"));
        assertEquals("PARKNSHOP", shopping.items().getFirst().selectedRetailerCode());
        assertEquals(0, shopping.items().getFirst().lineTotal().compareTo(new java.math.BigDecimal("18.00")));
        mvc.perform(post("/list/items/P000000124/update").param("quantity", "2")
                        .param("retailerCode", "UNKNOWN")).andExpect(flash().attributeExists("error"));
        assertEquals("PARKNSHOP", shopping.items().getFirst().selectedRetailerCode());
    }
    @Test void dataIsANormalDestinationAndCategoryFiltersUseSourceLabels() throws Exception {
        catalog.importSample();
        mvc.perform(get("/data")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Price data &amp; updates")))
                .andExpect(content().string(not(containsString("COMING LATER"))));
        mvc.perform(get("/").param("category", "Milk / Milk drinks"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("P000000124")))
                .andExpect(content().string(not(containsString("P000000229"))))
                .andExpect(content().string(containsString("category-filter")));
        mvc.perform(get("/").param("q", "food").param("related", "true"))
                .andExpect(status().isOk()).andExpect(model().attribute("related", true));
    }
    @Test void newSearchDoesNotCarryRelatedModeFromThePreviousQuery() throws Exception {
        catalog.importSample();
        mvc.perform(get("/").param("q", "Bakery").param("related", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("related", true))
                .andExpect(content().string(containsString("P000000229")))
                .andExpect(content().string(not(containsString("name=\"related\""))));
        mvc.perform(get("/").param("q", "milk"))
                .andExpect(model().attribute("related", false))
                .andExpect(content().string(containsString("P000000124")));
    }

    @Test void blankQueryShowsCatalogueEvenWithAStaleRelatedParameter() throws Exception {
        catalog.importSample();
        for (String query : new String[]{"", "   "}) {
            mvc.perform(get("/").param("q", query).param("related", "true"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("related", false))
                    .andExpect(model().attribute("resultCount", 2))
                    .andExpect(content().string(containsString("P000000124")))
                    .andExpect(content().string(containsString("P000000229")));
        }
    }

    @Test void laterFeaturesAreClearlyUnavailable() throws Exception {
        for (String feature : new String[]{"saved", "updates", "ask"}) {
            mvc.perform(get("/planned/" + feature)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("PLANNED · NOT AVAILABLE YET")))
                    .andExpect(content().string(not(containsString("chat-form"))));
        }
        mvc.perform(get("/data")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No automatic refresh runs at startup")));
    }
    @Test void sourceTextIsEscapedAndSearchPaginationIsBounded() throws Exception {
        catalog.importSample();
        mvc.perform(get("/").param("q", "<script>alert(1)</script>").param("page", "2147483647"))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("value=\"<script>"))))
                .andExpect(content().string(containsString("No matching products")));
        mvc.perform(get("/").param("page", "-1")).andExpect(status().isOk());
    }
}
