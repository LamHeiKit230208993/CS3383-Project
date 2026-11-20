package com.pricewatch.web;

import com.pricewatch.budget.BudgetService;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.SearchResults;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HelloControllerModelTest {
    private final CatalogService catalog = mock(CatalogService.class);
    private final ShoppingService shopping = mock(ShoppingService.class);
    private final BudgetService budgets = mock(BudgetService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        View emptyView = (model, request, response) -> {};
        mvc = MockMvcBuilders.standaloneSetup(new HelloController(catalog, shopping, budgets, new StaticMessageSource()))
                .setViewResolvers((name, locale) -> emptyView)
                .build();
    }

    @Test
    void listUsesOneReadForRowsAndSummary() throws Exception {
        when(shopping.lists()).thenReturn(List.of(new ShoppingService.ListChoice(null, "My list")));
        var items = List.of(new ItemView(null, 2, false, new BigDecimal("12.50"),
                "JASONS", new BigDecimal("25.00"), "JASONS"),
                new ItemView(null, 3, false, null, "AEON", null, "AEON"));
        when(shopping.items()).thenReturn(items);

        var result = mvc.perform(get("/list")).andExpect(status().isOk())
                .andExpect(model().attribute("summary", new Summary(new BigDecimal("25.0000"), 1, 5)))
                .andReturn();

        assertSame(items, result.getModelAndView().getModel().get("items"));
        verify(shopping).items();
        verify(shopping).lists();
        verify(budgets).forSummary(new Summary(new BigDecimal("25.0000"), 1, 5));
        verifyNoMoreInteractions(shopping);
    }

    @Test
    void discoverSharesOneCartReadAcrossPreviewAndBudget() throws Exception {
        when(catalog.searchResults("", "")).thenReturn(new SearchResults(List.of(), List.of(), List.of(), Map.of()));
        var line = new ItemView(null, 2, false, new BigDecimal("7.50"), "AEON", new BigDecimal("15.00"), "AEON");
        var items = List.of(line, line, line, line);
        when(shopping.items()).thenReturn(items);
        var summary = new Summary(new BigDecimal("60.0000"), 0, 8);

        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(model().attribute("cartLineCount", 4))
                .andExpect(model().attribute("previewItems", items.subList(0, 3)))
                .andExpect(model().attribute("summary", summary));

        verify(budgets).forSummary(summary);
        verify(shopping).items();
        verifyNoMoreInteractions(shopping);
    }

    @Test
    void dataAndPlannedPagesDoNotReadTheShoppingList() throws Exception {
        mvc.perform(get("/data")).andExpect(status().isOk());
        mvc.perform(get("/planned/saved")).andExpect(status().isOk());
        verifyNoInteractions(shopping);
    }

    @Test
    void listMutationDoesNotReadAnUnusedSummary() throws Exception {
        mvc.perform(post("/list/items").param("code", "P000000124")
                        .param("quantity", "2").param("retailerCode", "JASONS"))
                .andExpect(view().name("redirect:/list"));
        verify(shopping).add("P000000124", 2, "JASONS");
        verifyNoMoreInteractions(shopping);
    }
}

