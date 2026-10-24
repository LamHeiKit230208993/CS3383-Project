package com.pricewatch.catalog;

import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ShoppingServiceTest {
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;

    @BeforeEach
    void clearDatabase() {
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    void listLoadsProductsAndOffersInOneQuery() {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        shopping.add("P000000229", 3);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            var items = shopping.items();
            assertEquals(2, items.size());
            assertTrue(items.stream().allMatch(item -> !item.product().listings().isEmpty()));
            assertTrue(items.stream().anyMatch(item -> item.product().listings().stream()
                    .anyMatch(listing -> !listing.offers().isEmpty())));
            assertEquals(1L, statistics.getPrepareStatementCount(),
                    "The list query must also fetch products, listings, and offers.");
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }

    @Test
    void addUpdateAndRemovePersistQuantitiesAndCheckedState() {
        catalog.importSample();

        shopping.add("P000000124", 2);
        shopping.add("P000000124", 3);
        ItemView added = shopping.items().getFirst();
        assertEquals(5, added.quantity());
        assertNull(added.selectedRetailerCode());
        assertEquals("AEON", added.retailerCode());
        assertEquals(new BigDecimal("7.5000"), added.unitPrice());
        assertEquals(new BigDecimal("37.5000"), added.lineTotal());

        shopping.update("P000000124", 4, true);
        assertEquals(true, shopping.items().getFirst().checked());
        assertEquals(new BigDecimal("30.0000"), shopping.summary().knownSubtotal());
        assertEquals(4, shopping.summary().totalQuantity());

        shopping.remove("P000000124");
        assertTrue(shopping.items().isEmpty());
        assertEquals(BigDecimal.ZERO.setScale(4), shopping.summary().knownSubtotal());
    }

    @Test
    void sameSelectedRetailerAddsAndLegacyUpdatePreservesSelection() {
        catalog.importSample();

        shopping.add("P000000124", 1, "JASONS");
        shopping.add("P000000124", 1, "JASONS");
        ItemView selected = shopping.items().getFirst();
        assertEquals(2, selected.quantity());
        assertEquals("JASONS", selected.selectedRetailerCode());
        assertEquals("JASONS", selected.retailerCode());
        assertEquals(new BigDecimal("12.5000"), selected.unitPrice());
        assertEquals(new BigDecimal("25.0000"), selected.lineTotal());

        shopping.update("P000000124", 2, true);
        selected = shopping.items().getFirst();
        assertTrue(selected.checked());
        assertEquals("JASONS", selected.selectedRetailerCode());
        assertEquals(new BigDecimal("25.0000"), shopping.summary().knownSubtotal());
    }

    @Test
    void updateCanChangeRetailerAndBlankChoiceReturnsToAutomatic() {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");

        shopping.update("P000000124", 2, true, "WELLCOME");
        ItemView changed = shopping.items().getFirst();
        assertEquals("WELLCOME", changed.selectedRetailerCode());
        assertEquals(new BigDecimal("12.0000"), changed.unitPrice());
        assertEquals(new BigDecimal("24.0000"), changed.lineTotal());

        shopping.update("P000000124", 2, true, " ");
        ItemView automatic = shopping.items().getFirst();
        assertNull(automatic.selectedRetailerCode());
        assertEquals("AEON", automatic.retailerCode());
        assertEquals(new BigDecimal("7.5000"), automatic.unitPrice());
        assertEquals(new BigDecimal("15.0000"), automatic.lineTotal());
    }

    @Test
    void addingSameProductWithAnotherRetailerRequiresChangingItInTheList() {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> shopping.add("P000000124", 1, "WELLCOME"));

        assertTrue(error.getMessage().contains("already in your list with another retailer"));
        ItemView item = shopping.items().getFirst();
        assertEquals(2, item.quantity());
        assertEquals("JASONS", item.selectedRetailerCode());
    }

    @Test
    void explicitRetailerMustHaveAKnownPrice() {
        accept(product("KNOWN", "[{\"supermarketCode\":\"STORE\",\"price\":\"2.25\"}]"),
                product("UNKNOWN", "[{\"supermarketCode\":\"STORE\",\"price\":null}]"));

        assertThrows(IllegalArgumentException.class, () -> shopping.add("KNOWN", 1, "MISSING"));
        assertThrows(IllegalArgumentException.class, () -> shopping.add("UNKNOWN", 1, "STORE"));
        assertEquals(0, listRepository.count());
        assertTrue(shopping.items().isEmpty());
    }

    @Test
    void unavailableSelectedRetailerStaysPinnedAndHasUnknownTotal() {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        accept(product("P000000124", "[{\"supermarketCode\":\"OTHER\",\"price\":\"1.00\"}]"));

        shopping.update("P000000124", 3, true, "JASONS");

        ItemView retained = shopping.items().getFirst();
        assertEquals("JASONS", retained.selectedRetailerCode());
        assertEquals("JASONS", retained.retailerCode());
        assertNull(retained.unitPrice());
        assertNull(retained.lineTotal());
        assertEquals(1, shopping.summary().unknownItemCount());
    }

    @Test
    void validatesQuantityAndMarksTotalsIncompleteForUnknownPrices() {
        accept(
                product("KNOWN", "[{\"supermarketCode\":\"STORE\",\"price\":\"2.25\"},"
                        + "{\"supermarketCode\":\"AEON\",\"price\":\"2.25\"}]"),
                product("UNKNOWN", "[{\"supermarketCode\":\"STORE\",\"price\":null}]"));

        shopping.add("KNOWN", 2);
        shopping.add("UNKNOWN", 3);
        assertThrows(IllegalArgumentException.class, () -> shopping.add("KNOWN", 998));
        assertThrows(IllegalArgumentException.class, () -> shopping.add("KNOWN", 0));
        assertThrows(IllegalArgumentException.class, () -> shopping.update("KNOWN", 1000, false));

        Summary summary = shopping.summary();
        assertEquals(new BigDecimal("4.5000"), summary.knownSubtotal());
        assertEquals(1, summary.unknownItemCount());
        assertEquals(5, summary.totalQuantity());
        assertEquals("AEON", shopping.items().stream().filter(item -> item.product().code().equals("KNOWN"))
                .findFirst().orElseThrow().retailerCode());
        ItemView unknown = shopping.items().stream().filter(item -> item.product().code().equals("UNKNOWN"))
                .findFirst().orElseThrow();
        assertNull(unknown.unitPrice());
        assertNull(unknown.lineTotal());
    }

    @Test
    void savedItemsStayWhenTheirProductLeavesTheLatestCatalogue() {
        catalog.importSample();
        shopping.add("P000000124", 4);
        shopping.update("P000000124", 4, true);
        accept(product("OTHER", "[]"));

        ItemView retained = shopping.items().getFirst();
        assertEquals("P000000124", retained.product().code());
        assertFalse(retained.product().active());
        assertEquals(4, retained.quantity());
        assertTrue(retained.checked());
        assertNull(retained.unitPrice());
        assertEquals(1, shopping.summary().unknownItemCount());
    }

    @Test
    void invalidImportLeavesSavedListUnchanged() {
        catalog.importSample();
        shopping.add("P000000124", 2);
        String priorChecksum = catalog.status().checksum();

        assertThrows(CatalogImportException.class, () -> catalog.accept("[]".getBytes(StandardCharsets.UTF_8), "LIVE", "test"));

        assertEquals(priorChecksum, catalog.status().checksum());
        assertEquals(2, shopping.items().getFirst().quantity());
        assertFalse(shopping.items().getFirst().checked());
    }

    @Test
    void concurrentAddsToEmptyListCreateOneListAndAggregateQuantity() throws Exception {
        catalog.importSample();
        assertEquals(0, listRepository.count());
        assertTrue(shopping.items().isEmpty());

        int concurrentAdds = 12;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentAdds);
        CountDownLatch ready = new CountDownLatch(concurrentAdds);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<? extends Future<?>> additions = IntStream.range(0, concurrentAdds)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out waiting to start concurrent add.");
                        }
                        shopping.add("P000000124", 1);
                        return null;
                    }))
                    .toList();

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> addition : additions) {
                addition.get(10, TimeUnit.SECONDS);
            }

            assertEquals(1, listRepository.count());
            assertEquals(1, shopping.items().size());
            assertEquals(concurrentAdds, shopping.items().getFirst().quantity());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private void accept(String... records) {
        catalog.accept(json(records), "SAMPLE", "test");
    }

    private byte[] json(String... records) {
        return ("[" + String.join(",", records) + "]").getBytes(StandardCharsets.UTF_8);
    }

    private String product(String code, String prices) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat3Name":{"en":"Fresh","zh-Hant":"鮮奶","zh-Hans":"鲜奶"},
                "prices":%s,"offers":[]}
                """.formatted(code, code, prices).trim();
    }
}
