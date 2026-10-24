package com.pricewatch.web;

import com.pricewatch.domain.ListItem;
import com.pricewatch.domain.ShoppingList;
import com.pricewatch.importing.CatalogueImporter;
import com.pricewatch.store.ShoppingListStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ListFlowTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ShoppingListStore listStore;

    @Autowired
    private CatalogueImporter importer;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        importer.importSample();
        listStore.save(new ShoppingList(ShoppingList.DEFAULT_NAME, java.util.List.of()));
    }

    @Test
    void addItemIsStoredAndSurvivesReload() throws Exception {
        mockMvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/list"));

        ShoppingList reloaded = listStore.load();

        assertThat(reloaded.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductCode()).isEqualTo("P000000124");
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.isChecked()).isFalse();
        });
    }

    @Test
    void addingTheSameProductTwiceAddsQuantityNotRows() throws Exception {
        mockMvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "1"));
        mockMvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "3"));

        assertThat(listStore.load().getItems()).singleElement()
                .extracting(ListItem::getQuantity).isEqualTo(4);
    }

    @Test
    void invalidQuantityDoesNotChangeTheList() throws Exception {
        mockMvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "0"))
                .andExpect(redirectedUrl("/list"));

        assertThat(listStore.load().getItems()).isEmpty();
    }

    @Test
    void unknownProductIsRejected() throws Exception {
        mockMvc.perform(post("/list/items").param("code", "NOPE").param("quantity", "1"))
                .andExpect(redirectedUrl("/"));

        assertThat(listStore.load().getItems()).isEmpty();
    }

    @Test
    void togglePersistsCheckedState() throws Exception {
        mockMvc.perform(post("/list/items").param("code", "P000000229").param("quantity", "1"));
        mockMvc.perform(post("/list/items/P000000229/toggle").param("checked", "true"))
                .andExpect(redirectedUrl("/list"));

        assertThat(listStore.load().item("P000000229").isChecked()).isTrue();

        mockMvc.perform(post("/list/items/P000000229/toggle").param("checked", "false"));

        assertThat(listStore.load().item("P000000229").isChecked()).isFalse();
    }

    @Test
    void changingQuantityPersists() throws Exception {
        mockMvc.perform(post("/list/items").param("code", "P000000125").param("quantity", "1"));
        mockMvc.perform(post("/list/items/P000000125/quantity").param("quantity", "5"))
                .andExpect(redirectedUrl("/list"));

        assertThat(listStore.load().item("P000000125").getQuantity()).isEqualTo(5);
    }

    @Test
    void togglingAMissingItemLeavesTheListAlone() throws Exception {
        mockMvc.perform(post("/list/items/NOPE/toggle").param("checked", "true"))
                .andExpect(redirectedUrl("/list"));

        assertThat(listStore.load().getItems()).isEmpty();
    }

    @Test
    void removeDeletesTheRow() throws Exception {
        mockMvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "2"));
        mockMvc.perform(post("/list/items").param("code", "P000000229").param("quantity", "1"));

        mockMvc.perform(post("/list/items/P000000124/remove"))
                .andExpect(redirectedUrl("/list"));

        assertThat(listStore.load().getItems()).singleElement()
                .extracting(ListItem::getProductCode).isEqualTo("P000000229");
    }

    @Test
    void removingAMissingItemLeavesTheListAlone() throws Exception {
        mockMvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "1"));

        mockMvc.perform(post("/list/items/NOPE/remove"))
                .andExpect(redirectedUrl("/list"));

        assertThat(listStore.load().getItems()).hasSize(1);
    }

    @Test
    void removingTheLastItemLeavesAnEmptyList() throws Exception {
        mockMvc.perform(post("/list/items").param("code", "P000000124").param("quantity", "1"));
        mockMvc.perform(post("/list/items/P000000124/remove"));

        assertThat(listStore.load().getItems()).isEmpty();
    }
}