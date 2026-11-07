package com.pricewatch.tracking;

import com.pricewatch.MutableClockConfig;
import com.pricewatch.catalog.CatalogImportException;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingChoiceRepository;
import com.pricewatch.tracking.repository.WishlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MutableClockConfig.class)
class TrackingServiceTest {
    @Autowired private CatalogService catalog;
    @Autowired private TrackingService tracking;
    @Autowired private WishlistEntryRepository wishlist;
    @Autowired private TrackingChoiceRepository choices;
    @Autowired private PriceObservationRepository observations;
    @Autowired private ProductRepository productRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;

    @BeforeEach
    void clear() {
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        observations.deleteAllInBatch();
        choices.deleteAllInBatch();
        wishlist.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    void savingDoesNotStartTracking() {
        catalog.importSample();
        tracking.save("P000000124");
        assertTrue(tracking.isSaved("P000000124"));
        assertFalse(tracking.isTracked("P000000124"));
        assertEquals(0, observations.count());
    }

    @Test
    void enablingTrackingSeedsOneChoicePerRetailer() {
        catalog.importSample();
        tracking.enableTracking("P000000124");
        var product = productRepository.findWithListingsByCode("P000000124").orElseThrow();
        assertEquals(product.getListings().size(), tracking.activeChoices().stream()
                .filter(c -> c.getProductCode().equals("P000000124")).count());
        assertTrue(tracking.isTracked("P000000124"));
    }

    @Test
    void trackingStoresOlderPricesOnSuccessfulImportOnly() {
        catalog.importSample();
        tracking.enableTracking("P000000124");
        int perRetailer = observationCount("P000000124");

        accept(product("P000000124", "[{\"supermarketCode\":\"AEON\",\"price\":\"7.10\"},"
                + "{\"supermarketCode\":\"JASONS\",\"price\":\"11.00\"}]"));
        assertTrue(observationCount("P000000124") > perRetailer, "new observations recorded");
        var history = tracking.history("P000000124", "AEON");
        assertEquals(new BigDecimal("7.1000"), history.get(history.size() - 1).getPrice());

        int before = (int) observations.count();
        assertThrows(CatalogImportException.class,
                () -> catalog.accept("[]".getBytes(StandardCharsets.UTF_8), "LIVE", "test"));
        assertEquals(before, (int) observations.count(), "failed import adds nothing");
    }

    @Test
    void repeatedIdenticalImportAddsNoNewObservation() {
        catalog.importSample();
        tracking.enableTracking("P000000124");
        byte[] same = sampleBytes();
        catalog.accept(same, "SAMPLE", "test");
        int afterFirst = (int) observations.count();
        catalog.accept(same, "SAMPLE", "test");
        assertEquals(afterFirst, (int) observations.count());
    }

    @Test
    void stoppingStopsCollectionAndReenableStartsNewPeriod() {
        catalog.importSample();
        tracking.enableTracking("P000000124");
        catalog.accept(sampleBytes(), "SAMPLE", "t1");
        long period1 = choices.findAllByProductCode("P000000124").get(0).getPeriodId();

        tracking.disableTracking("P000000124");
        assertFalse(tracking.isTracked("P000000124"));
        int frozen = (int) observations.count();
        accept(product("P000000124", "[{\"supermarketCode\":\"AEON\",\"price\":\"1.00\"}]"));
        assertEquals(frozen, (int) observations.count(), "no collection while stopped");

        tracking.enableTracking("P000000124");
        long period2 = choices.findAllByProductCode("P000000124").get(0).getPeriodId();
        assertTrue(period2 > period1, "re-enable starts a new period");
    }

    @Test
    void unknownPriceIsRecordedAsProvenanceOnly() {
        catalog.importSample();
        tracking.enableTracking("P000000124");
        accept(product("P000000124", "[{\"supermarketCode\":\"AEON\",\"price\":null}]"));
        var history = tracking.history("P000000124", "AEON");
        var latest = history.get(history.size() - 1);
        assertFalse(latest.isKnown());
        assertNull(latest.getPrice());
    }

    private int observationCount(String code) {
        return choices.findAllByEnabledTrue().stream()
                .filter(c -> c.getProductCode().equals(code))
                .mapToInt(c -> tracking.history(code, c.getRetailerCode()).size()).sum();
    }

    private byte[] sampleBytes() {
        try (var in = new org.springframework.core.io.ClassPathResource("/samples/pricewatch-products.json").getInputStream()) {
            return in.readAllBytes();
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    private void accept(String... records) {
        catalog.accept(("[" + String.join(",", records) + "]").getBytes(StandardCharsets.UTF_8), "SAMPLE", "test");
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
