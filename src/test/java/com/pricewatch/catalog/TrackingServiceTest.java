package com.pricewatch.catalog;

import com.pricewatch.catalog.CatalogImportException;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.tracking.TrackedProductView;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingChoiceRepository;
import com.pricewatch.tracking.repository.WishlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TrackingServiceTest {
    @Autowired private CatalogService catalog;
    @Autowired private TrackingService tracking;
    @Autowired private WishlistEntryRepository wishlistRepository;
    @Autowired private TrackingChoiceRepository trackingRepository;
    @Autowired private PriceObservationRepository observationRepository;
    @Autowired private com.pricewatch.shopping.repository.ShoppingListItemRepository itemRepository;
    @Autowired private com.pricewatch.shopping.repository.ShoppingListRepository listRepository;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void clearDatabase() {
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        wishlistRepository.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    void savingDoesNotStartTracking() {
        accept(product("P1", "\"2.25\"", "\"3.00\""));

        tracking.save("P1");

        assertTrue(tracking.isSaved("P1"));
        assertFalse(tracking.isTracked("P1"));
        assertEquals(0, observationRepository.countByProductCode("P1"));
    }

    @Test
    void trackingRecordsOneObservationPerPricedPairPerImport() {
        accept(product("P1", "\"2.25\"", "\"3.00\""));
        tracking.enableTracking("P1");
        assertTrue(tracking.isTracked("P1"));

        // A successful, changed import produces one observation for each priced retailer.
        accept(product("P1", "\"2.20\"", "\"3.00\""));
        assertEquals(2, observationRepository.countByProductCode("P1"));

        // A further changed import records again for each priced retailer.
        accept(product("P1", "\"2.10\"", "\"2.90\""));
        assertEquals(4, observationRepository.countByProductCode("P1"));
    }

    @Test
    void unchangedRepeatedFileDoesNotDuplicateObservations() {
        accept(product("P1", "\"2.25\"", "\"3.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"2.25\"", "\"3.00\""));
        assertEquals(2, observationRepository.countByProductCode("P1"));

        // The identical file yields the same checksum, so nothing is stored twice.
        accept(product("P1", "\"2.25\"", "\"3.00\""));
        assertEquals(2, observationRepository.countByProductCode("P1"));
    }

    @Test
    void unknownPriceAddsNoObservation() {
        accept(product("P1", "\"2.25\"", "null"));
        tracking.enableTracking("P1");

        accept(product("P1", "\"2.25\"", "null"));
        assertEquals(1, observationRepository.countByProductCode("P1"));
        assertTrue(tracking.trackedProducts().getFirst().trackedRetailers().contains("STORE"));
        assertFalse(tracking.trackedProducts().getFirst().trackedRetailers().contains("EMPTY"));
    }

    @Test
    void disablingTrackingStopsCollectionButKeepsHistory() {
        accept(product("P1", "\"2.25\"", "\"3.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"2.25\"", "\"3.00\""));
        int retained = (int) observationRepository.countByProductCode("P1");
        assertEquals(2, retained);

        tracking.disableTracking("P1");
        assertFalse(tracking.isTracked("P1"));

        accept(product("P1", "\"1.00\"", "\"1.00\""));
        assertEquals(retained, observationRepository.countByProductCode("P1"));
    }

    @Test
    void failedImportAddsNoObservations() {
        accept(product("P1", "\"2.25\"", "\"3.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"2.25\"", "\"3.00\""));
        long after = observationRepository.countByProductCode("P1");

        try {
            catalog.accept("[]".getBytes(StandardCharsets.UTF_8), "LIVE", "test");
        } catch (CatalogImportException expected) {
            // invalid import is rejected
        }
        assertEquals(after, observationRepository.countByProductCode("P1"));
    }

    @Test
    void observationsSurviveCatalogueReplacementAndKeepOfferSnapshot() {
        accept(withOffer(product("P1", "\"2.25\"", "\"3.00\"")));
        tracking.enableTracking("P1");
        accept(withOffer(product("P1", "\"2.25\"", "\"3.00\"")));

        // Replace with a different product set; listings are deleted by replacement.
        accept(product("OTHER", "\"9.99\"", "\"9.99\""));

        List<TrackedProductView> tracked = tracking.trackedProducts();
        assertEquals(1, tracked.size());
        TrackedProductView view = tracked.getFirst();
        assertEquals("P1", view.productCode());
        assertFalse(view.active());
        assertEquals(2, view.observations().size());
        assertTrue(view.observations().stream().anyMatch(observation ->
                observation.offerSnapshot() != null && observation.offerSnapshot().contains("Buy 2")));
    }

    @Test
    void removingSavedProductKeepsTrackingHistory() {
        accept(product("P1", "\"2.25\"", "\"3.00\""));
        tracking.save("P1");
        tracking.enableTracking("P1");
        accept(product("P1", "\"2.25\"", "\"3.00\""));

        tracking.unsave("P1");

        assertFalse(tracking.isSaved("P1"));
        assertTrue(tracking.isTracked("P1"));
        assertFalse(observationRepository.findAllByProductCodeOrderByRetrievedAtDescIdDesc("P1").isEmpty());
    }

    private void accept(String... records) {
        catalog.accept(("[" + String.join(",", records) + "]").getBytes(StandardCharsets.UTF_8),
                "SAMPLE", "test");
    }

    private String withOffer(String record) {
        return record.replace("\"offers\":[]",
                "\"offers\":[{\"supermarketCode\":\"STORE\",\"en\":\"Buy 2 save\","
                        + "\"zh-Hant\":\"買2件\",\"zh-Hans\":\"买2件\"}]");
    }

    private String product(String code, String storePrice, String emptyPrice) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat3Name":{"en":"Fresh","zh-Hant":"鮮奶","zh-Hans":"鲜奶"},
                "prices":[{"supermarketCode":"STORE","price":%s},{"supermarketCode":"EMPTY","price":%s}],
                "offers":[]}
                """.formatted(code, code, storePrice, emptyPrice).trim();
    }
}
