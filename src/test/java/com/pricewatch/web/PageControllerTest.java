package com.pricewatch.web;

import com.pricewatch.domain.Product;
import com.pricewatch.store.CatalogueStore;
import com.pricewatch.store.ShoppingListStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PageControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private com.pricewatch.importing.CatalogueImporter importer;

    @Autowired
    private CatalogueStore catalogueStore;

    @Autowired
    private ShoppingListStore listStore;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        importer.importSample();
    }

    @Test
    void homeListsImportedProducts() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Crustless Sandwich Bread 6 slices")))
                .andExpect(content().string(containsString("Retrieved")))
                .andExpect(content().string(containsString("source date unknown")));
    }

    @Test
    void searchFindsByTraditionalChineseName() throws Exception {
        mockMvc.perform(get("/").param("q", "\u9bae\u725b\u5976"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("P000000124")))
                .andExpect(content().string(not(containsString("Crustless Sandwich Bread 6 slices"))));
    }

    @Test
    void searchFindsBySimplifiedChineseName() throws Exception {
        mockMvc.perform(get("/").param("q", "\u9c9c\u725b\u5976"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("P000000124")))
                .andExpect(content().string(containsString("P000000125")));
    }

    @Test
    void compareShowsTiesAndRetrievedTime() throws Exception {
        mockMvc.perform(get("/compare/P000000229"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tied prices")))
                .andExpect(content().string(containsString("Buy 3 at $30.00")))
                .andExpect(content().string(containsString("11.50")));
    }

    @Test
    void compareOfUnknownCodeRedirectsHome() throws Exception {
        mockMvc.perform(get("/compare/NOPE"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void missingPriceIsLabelledAndNotZero() throws Exception {
        Product product = new Product("P-UNKNOWN-PRICE",
                new com.pricewatch.domain.LocalisedName("Brand", "b", "b"),
                new com.pricewatch.domain.LocalisedName("No price item", "n", "n"),
                new com.pricewatch.domain.LocalisedName("c", "c", "c"),
                new com.pricewatch.domain.LocalisedName("c", "c", "c"),
                new com.pricewatch.domain.LocalisedName("c", "c", "c"),
                java.util.List.of(new com.pricewatch.domain.Listing("AEON", null,
                        new com.pricewatch.domain.LocalisedName("", "", ""))));
        catalogueStore.replace(new com.pricewatch.domain.Catalogue(java.time.Instant.now(), null, "test://x",
                java.util.List.of(product)));

        mockMvc.perform(get("/compare/P-UNKNOWN-PRICE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No price listed")))
                .andExpect(content().string(containsString("No listed price")))
                .andExpect(content().string(not(containsString("HK$0.00"))));
    }

    @Test
    void listPageStartsEmpty() throws Exception {
        listStore.save(new com.pricewatch.domain.ShoppingList(
                com.pricewatch.domain.ShoppingList.DEFAULT_NAME, java.util.List.of()));

        mockMvc.perform(get("/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your list is empty")));
    }

    @Test
    void listRowShowsSubtotalForTheQuantity() throws Exception {
        listStore.save(new com.pricewatch.domain.ShoppingList(
                com.pricewatch.domain.ShoppingList.DEFAULT_NAME, java.util.List.of()));
        var list = listStore.load();
        list.add("P000000124", 2, java.time.Instant.now());
        listStore.save(list);

        mockMvc.perform(get("/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("HK$7.50")))
                .andExpect(content().string(containsString("HK$15.00")));
    }

    @Test
    void listRowHasARemoveControl() throws Exception {
        listStore.save(new com.pricewatch.domain.ShoppingList(
                com.pricewatch.domain.ShoppingList.DEFAULT_NAME, java.util.List.of()));
        var list = listStore.load();
        list.add("P000000229", 1, java.time.Instant.now());
        listStore.save(list);

        mockMvc.perform(get("/list"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/list/items/P000000229/remove")))
                .andExpect(content().string(containsString("Remove Crustless Sandwich Bread 6 slices from the list")));
    }
}