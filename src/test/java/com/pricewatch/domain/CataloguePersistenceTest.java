package com.pricewatch.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CataloguePersistenceTest {

    @Autowired
    private com.pricewatch.store.CatalogueStore store;

    @Autowired
    private com.pricewatch.store.ShoppingListStore listStore;

    @Test
    void savedCatalogueCanBeReadBack() {
        Product product = Fixtures.product("P-TEST-1", "milk",
                Fixtures.listing("AEON", "7.50"));
        store.replace(new Catalogue(Instant.parse("2026-10-20T10:00:00Z"), null, "test://sample", List.of(product)));

        Catalogue reloaded = store.get();

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getSourceDate()).isNull();
        assertThat(reloaded.getImportedAt()).isEqualTo(Instant.parse("2026-10-20T10:00:00Z"));
        assertThat(reloaded.product("P-TEST-1").getListings()).singleElement()
                .extracting(Listing::getPrice).isEqualTo(new BigDecimal("7.50"));
    }

    @Test
    void missingPriceStaysNullInStorage() {
        Product product = Fixtures.product("P-TEST-2", "laundry",
                Fixtures.listing("WATSONS", null));
        store.replace(new Catalogue(Instant.now(), LocalDate.parse("2026-10-19"), "test://sample", List.of(product)));

        Catalogue reloaded = store.get();

        assertThat(reloaded.product("P-TEST-2").getListings().get(0).getPrice()).isNull();
        assertThat(reloaded.product("P-TEST-2").getListings().get(0).hasPrice()).isFalse();
    }

    @Test
    void savedListSurvivesReload() {
        ShoppingList list = listStore.load();
        list.add("P-TEST-3", 2, Instant.now());
        list.toggle("P-TEST-3", true);
        listStore.save(list);

        ShoppingList reloaded = listStore.load();

        assertThat(reloaded.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductCode()).isEqualTo("P-TEST-3");
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.isChecked()).isTrue();
        });
    }
}
