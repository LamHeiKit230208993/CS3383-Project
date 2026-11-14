package com.pricewatch.budget;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.web.BudgetController;
import com.pricewatch.web.HelloController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class BudgetControllerTest {
    private final BudgetService budgets = mock(BudgetService.class);
    private final CatalogService catalog = mock(CatalogService.class);
    private final ShoppingService shopping = mock(ShoppingService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new BudgetController(budgets, catalog, new HelloController(catalog, shopping))).build();

    @BeforeEach
    void defaults() {
        when(catalog.status()).thenReturn(null);
        when(budgets.view()).thenReturn(new BudgetView(BudgetPeriod.WEEKLY, null,
                new BigDecimal("0.0000"), 0, List.of(), BudgetState.NOT_SET));
    }

    @Test
    void getShowsBudgetSceneAndSharedModel() throws Exception {
        mvc.perform(get("/budget"))
                .andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(model().attribute("scene", "budget"))
                .andExpect(model().attribute("title", "Budget"))
                .andExpect(model().attributeExists("budget", "format"))
                .andExpect(model().attributeDoesNotExist("importStatus"));
    }

    @Test
    void validSavePersistsAndRedirectsWithNotice() throws Exception {
        when(budgets.save("MONTHLY", "100.00")).thenReturn(BudgetPeriod.MONTHLY);

        mvc.perform(post("/budget").param("period", "MONTHLY").param("limit", "100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/budget"))
                .andExpect(flash().attribute("notice", "Monthly budget saved on this device."));

        verify(budgets).save("MONTHLY", "100.00");
    }

    @Test
    void invalidSaveKeepsTheFormAndDoesNotRedirect() throws Exception {
        when(budgets.save("YEARLY", "-1")).thenThrow(new IllegalArgumentException("Choose a weekly or monthly budget period."));

        mvc.perform(post("/budget").param("period", "YEARLY").param("limit", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("market-desk"))
                .andExpect(model().attribute("error", "Choose a weekly or monthly budget period."))
                .andExpect(model().attribute("budgetFormPeriod", "YEARLY"))
                .andExpect(model().attribute("budgetFormLimit", "-1"));

        verify(budgets).save("YEARLY", "-1");
    }
}
