package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HelloControllerModelTest {
    private final CatalogService catalog = mock(CatalogService.class);
    private final ShoppingService shopping = mock(ShoppingService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        View emptyView = (model, request, response) -> {};
        ViewFormat format = new ViewFormat(ZoneId.of("Asia/Hong_Kong"));
        mvc = MockMvcBuilders.standaloneSetup(new HelloController(catalog, shopping))
                .setControllerAdvice(new GlobalModelAttributes(catalog, format))
                .setViewResolvers((name, locale) -> emptyView)
                .build();
    }

    @Test
    void listUsesOneReadForRowsAndSummary() throws Exception {
        var items = List.of(new ItemView(null, 2, false, new BigDecimal("12.50"),
                "JASONS", new BigDecimal("25.00"), "JASONS"),
                new ItemView(null, 3, false, null, "AEON", null, "AEON"));
        when(shopping.items()).thenReturn(items);

        var result = mvc.perform(get("/list")).andExpect(status().isOk())
                .andExpect(model().attribute("summary", new Summary(new BigDecimal("25.0000"), 1, 5)))
                .andReturn();

        assertSame(items, result.getModelAndView().getModel().get("items"));
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

