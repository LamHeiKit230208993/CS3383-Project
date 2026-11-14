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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    void emptyCatalogueAndListRenderWithoutABudgetSet() throws Exception {
        mvc.perform(get("/budget"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No prices loaded")))
                .andExpect(content().string(containsString("Set a limit to compare")))
                .andExpect(content().string(containsString("No priced list items to group yet.")));
    }

    @Test
    void overLimitWithCompletePricesDoesNotClaimPricesAreMissing() throws Exception {
        catalog.importSample();
        shopping.add("P000000124", 1);
        budgets.save("WEEKLY", "0.00");

        mvc.perform(get("/budget"))
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

        mvc.perform(get("/budget"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The current list estimate is within your planned limit.")))
                .andExpect(content().string(not(containsString("The known subtotal is already above your limit."))));
    }
}
