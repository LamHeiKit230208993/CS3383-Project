package com.pricewatch.catalog;

import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.catalog.importer.ConsumerCouncilSourceClient;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CatalogServiceIntegrationTest {
    @Autowired private CatalogService catalog;
    @Autowired private ShoppingService shopping;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;
    @MockitoBean private ConsumerCouncilSourceClient sourceClient;

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
    void sampleImportIsIdempotentAndSearchesAllThreeNames() {
        ImportStatus first = catalog.importSample();
        Instant firstRetrievedAt = first.retrievedAt();
        ImportStatus second = catalog.importSample();

        assertEquals("SAMPLE", second.origin());
        assertNull(second.sourceDate());
        assertTrue(second.retrievedAt().compareTo(firstRetrievedAt) >= 0);
        assertEquals(2, second.productCount());
        assertEquals(8, second.listingCount());
        assertEquals(4, second.offerCount());
        assertEquals(8, listingRepository.count());
        assertEquals(4, offerRepository.count());
        assertEquals(List.of("P000000124"), catalog.search("100 %Fresh Milk 236mL").stream()
                .map(ProductView::code).toList());
        assertEquals(List.of("P000000124"), catalog.search("鮮牛奶 236毫升").stream()
                .map(ProductView::code).toList());
        assertEquals(List.of("P000000124"), catalog.search("鲜牛奶 236毫升").stream()
                .map(ProductView::code).toList());
        assertEquals("2 for $16.50", catalog.find("P000000124").listings().stream()
                .filter(listing -> listing.retailerCode().equals("WELLCOME"))
                .findFirst().orElseThrow().offers().getFirst().en());
    }

    @Test
    @Transactional
    void importedListingsAreVisibleToAReadInTheSameTransaction() {
        catalog.importSample();

        ProductView milk = catalog.find("P000000124");

        assertEquals(4, milk.listings().size());
        assertEquals("AEON", milk.lowestListings().getFirst().retailerCode());
    }

    @Test
    void differentPackCodesStaySeparateTiesShowAllStoresAndUnknownDoesNotWin() {
        String json = "[" + product("MILK-236", "Fresh Milk 236mL",
                "[{\"supermarketCode\":\"AEON\",\"price\":\"7.5000\"},"
                        + "{\"supermarketCode\":\"WELLCOME\",\"price\":\"7.50\"},"
                        + "{\"supermarketCode\":\"PARKNSHOP\",\"price\":\"\"}]", "[]") + ","
                + product("MILK-1L", "Fresh Milk 1L", "[{\"supermarketCode\":\"AEON\",\"price\":null}]", "[]") + "]";

        catalog.accept(json.getBytes(java.nio.charset.StandardCharsets.UTF_8), "SAMPLE", "test");

        ProductView small = catalog.find("MILK-236");
        ProductView large = catalog.find("MILK-1L");
        assertEquals(new BigDecimal("7.5000"), small.lowestPrice());
        assertEquals(List.of("AEON", "WELLCOME"), small.lowestListings().stream()
                .map(ListingView::retailerCode).toList());
        assertEquals(3, small.listings().size());
        assertNull(large.lowestPrice());
        assertTrue(large.lowestListings().isEmpty());
        assertEquals(List.of("MILK-1L", "MILK-236"), catalog.search("Fresh Milk").stream()
                .map(ProductView::code).toList());
    }

    @Test
    void rejectedImportKeepsLastAcceptedCatalogueAndMetadata() {
        ImportStatus accepted = catalog.importSample();

        assertThrows(CatalogImportException.class,
                () -> catalog.accept("[]".getBytes(java.nio.charset.StandardCharsets.UTF_8), "LIVE", "test"));

        assertEquals(accepted.checksum(), catalog.status().checksum());
        assertNotNull(catalog.find("P000000124"));
        assertEquals(2, catalog.search("").size());
    }

    @Test
    void databaseWriteFailureRollsBackDeletedCatalogueAndKeepsMetadataAndList() {
        catalog.importSample();
        shopping.add("P000000124", 2, "JASONS");
        shopping.update("P000000124", 2, true);
        var previousCatalogue = catalog.search("");
        var previousMetadata = catalog.status();
        var previousList = shopping.items();

        String replacement = "[" + product("P000000124", "Changed name that must roll back",
                "[{\"supermarketCode\":\"AEON\",\"price\":\"0.01\"}]", "[]") + ","
                + product("ROLLBACK-NEW", "New product that must roll back",
                "[{\"supermarketCode\":\"FAIL-STORE\",\"price\":\"1.00\"}]", "[]") + "]";

        // No enclosing test transaction: both the failed write and subsequent reads use real service transactions.
        jdbc.execute("alter table retailer_listings add constraint ck_test_replacement_failure "
                + "check (retailer_code <> 'FAIL-STORE')");
        try {
            var failure = assertThrows(DataIntegrityViolationException.class,
                    () -> catalog.accept(replacement.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            "LIVE", "test-replacement"));
            assertTrue(failure.getMostSpecificCause().getMessage().contains("CK_TEST_REPLACEMENT_FAILURE"));

            assertEquals(previousCatalogue, catalog.search(""));
            assertEquals(previousMetadata, catalog.status());
            assertEquals(previousList, shopping.items());
            assertEquals(2, productRepository.count());
            assertEquals(8, listingRepository.count());
            assertEquals(4, offerRepository.count());
            assertEquals(1, itemRepository.count());
            assertFalse(productRepository.existsById("ROLLBACK-NEW"));
        } finally {
            jdbc.execute("alter table retailer_listings drop constraint ck_test_replacement_failure");
        }
    }

    @Test
    void liveImportUsesFetchedBytesAndFetchFailureKeepsAcceptedData() {
        String liveJson = "[" + product("LIVE-1", "Fresh Item",
                "[{\"supermarketCode\":\"STORE\",\"price\":\"3.25\"}]", "[]") + "]";
        when(sourceClient.sourceUrl()).thenReturn("https://source.example/prices.json");
        when(sourceClient.fetch()).thenReturn(liveJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        ImportStatus live = catalog.refreshLive();
        when(sourceClient.fetch()).thenThrow(new CatalogImportException("source unavailable"));

        assertEquals("LIVE", live.origin());
        assertEquals("LIVE-1", catalog.search("").getFirst().code());
        assertThrows(CatalogImportException.class, catalog::refreshLive);
        assertEquals(live.checksum(), catalog.status().checksum());
        assertEquals("LIVE-1", catalog.search("").getFirst().code());
    }

    private String product(String code, String name, String prices, String offers) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat3Name":{"en":"Fresh","zh-Hant":"鮮奶","zh-Hans":"鲜奶"},
                "prices":%s,"offers":%s}
                """.formatted(code, name, prices, offers).trim();
    }
}
