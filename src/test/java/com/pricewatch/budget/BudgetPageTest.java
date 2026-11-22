package com.pricewatch.budget;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.shopping.ShoppingService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    @Autowired private EntityManager entityManager;

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

    @ParameterizedTest(name = "limit {0}, missing bread price {1}, combined warning {2}")
    @CsvSource({"0.00, false, false", "0.00, true, true", "100.00, true, false"})
    void additionalBudgetWarningRequiresBothAnOverLimitSubtotalAndAMissingPrice(
            String limit, boolean missingPrice, boolean combinedWarning) throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 1);
        shopping.add("P000000229", 1, "JASONS");
        if (missingPrice) {
            entityManager.flush();
            entityManager.createQuery("""
                    update RetailerListingEntity listing set listing.price = null
                    where listing.product.code = 'P000000229' and listing.retailerCode = 'JASONS'
                    """).executeUpdate();
            entityManager.clear();
        }
        budgets.save("WEEKLY", limit);

        var response = mvc.perform(get("/personal-data")).andExpect(status().isOk());
        var overLimit = containsString("The known subtotal is already above your limit.");
        var extraWarning = containsString("The full total may be higher.");
        response.andExpect(content().string(limit.equals("0.00") ? overLimit : not(overLimit)))
                .andExpect(content().string(combinedWarning ? extraWarning : not(extraWarning)));
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
