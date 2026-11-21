package com.pricewatch.budget;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:budget-web;DB_CLOSE_DELAY=-1",
        "spring.main.web-application-type=servlet"
})
@AutoConfigureMockMvc
@Transactional
class BudgetPageTest {
    @Autowired private MockMvc mvc;
    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private BudgetService budgets;

    @Test
    void personalDataRendersBudgetFormWithDefaultPeriodAndNoLimit() throws Exception {
        mvc.perform(get("/personal-data"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/personal-data/budget")))
                .andExpect(content().string(containsString("Save a planned limit to compare it with your list estimate.")))
                .andExpect(model().attribute("budgetFormPeriod", "WEEKLY"))
                .andExpect(model().attribute("budgetFormLimit", ""));
    }

    @Test
    void legacyBudgetLinkAndFormLeadToPersonalData() throws Exception {
        mvc.perform(get("/budget"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/personal-data#budget"));
        mvc.perform(post("/budget").param("period", "MONTHLY").param("limit", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/personal-data#budget"))
                .andExpect(flash().attribute("budgetNotice", "Monthly budget saved on this device."));
        mvc.perform(get("/personal-data"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("budgetFormPeriod", "MONTHLY"))
                .andExpect(content().string(containsString("value=\"100.0000\"")));
    }

    @Test
    void invalidSaveReturnsPersonalDataWithErrorAndDoesNotChangeSavedLimit() throws Exception {
        budgets.save("WEEKLY", "7.50");
        mvc.perform(post("/personal-data/budget").param("period", "MONTHLY").param("limit", "-1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("scene", "personal"))
                .andExpect(content().string(containsString("Enter a nonnegative limit")))
                .andExpect(model().attribute("budgetFormPeriod", "MONTHLY"))
                .andExpect(model().attribute("budgetFormLimit", "-1"));
        mvc.perform(get("/personal-data"))
                .andExpect(model().attribute("budgetFormPeriod", "WEEKLY"))
                .andExpect(content().string(containsString("value=\"7.5000\"")));
    }

    @Test
    void settingsRendersCombinedSectionsAndReturnsToBudgetAfterSave() throws Exception {
        mvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"personal-data\"")))
                .andExpect(content().string(containsString("id=\"budget\"")))
                .andExpect(content().string(containsString("id=\"display\"")))
                .andExpect(content().string(containsString("id=\"price-data\"")));
        mvc.perform(post("/personal-data/budget").param("fromSettings", "true")
                        .param("period", "MONTHLY").param("limit", "125.00"))
                .andExpect(redirectedUrl("/settings#budget"));
        mvc.perform(post("/personal-data/budget").param("fromSettings", "true")
                        .param("period", "MONTHLY").param("limit", "-1"))
                .andExpect(redirectedUrl("/settings#budget"))
                .andExpect(flash().attribute("budgetFormLimit", "-1"));
        mvc.perform(get("/settings"))
                .andExpect(model().attribute("budgetFormLimit", "125.0000"));
    }

    @Test
    void overLimitWithCompletePricesDoesNotClaimPricesAreMissing() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 1);
        budgets.save("WEEKLY", "0.00");

        mvc.perform(get("/personal-data"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The known subtotal is already above your limit.")))
                .andExpect(content().string(not(containsString("The full total may be higher."))))
                .andExpect(content().string(not(containsString("list items have no current selected price"))));
    }

    @Test
    void exactListTotalRendersWithinLimit() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 1);
        budgets.save("WEEKLY", "7.50");

        mvc.perform(get("/personal-data"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The current list estimate is within your planned limit.")))
                .andExpect(content().string(not(containsString("The known subtotal is already above your limit."))));
    }
}
