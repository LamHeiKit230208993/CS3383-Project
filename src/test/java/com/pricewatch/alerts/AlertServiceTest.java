package com.pricewatch.alerts;

import com.pricewatch.alerts.persistence.AlertRuleEntity;
import com.pricewatch.alerts.repository.AlertEventRepository;
import com.pricewatch.alerts.repository.AlertRuleRepository;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingChoiceRepository;
import com.pricewatch.tracking.repository.WishlistEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AlertServiceTest {
    static final ZoneId HK = ZoneId.of("Asia/Hong_Kong");
    static final MutableClock CLOCK = new MutableClock(Instant.parse("2026-11-10T04:00:00Z")); // 12:00 HKT, not quiet

    @TestConfiguration
    static class ClockOverride {
        @Bean
        @Primary
        Clock testClock() {
            return CLOCK;
        }
    }

    @Autowired private CatalogService catalog;
    @Autowired private TrackingService tracking;
    @Autowired private AlertService alerts;
    @Autowired private AlertRuleRepository ruleRepository;
    @Autowired private AlertEventRepository eventRepository;
    @Autowired private PriceObservationRepository observationRepository;
    @Autowired private TrackingChoiceRepository trackingRepository;
    @Autowired private WishlistEntryRepository wishlistRepository;
    @Autowired private ShoppingListItemRepository itemRepository;
    @Autowired private ShoppingListRepository listRepository;
    @Autowired private AcceptedImportRepository importRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private RetailerListingRepository listingRepository;
    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void clearDatabase() {
        eventRepository.deleteAllInBatch();
        ruleRepository.deleteAllInBatch();
        observationRepository.deleteAllInBatch();
        trackingRepository.deleteAllInBatch();
        wishlistRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        listRepository.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        CLOCK.set(Instant.parse("2026-11-10T04:00:00Z"));
    }

    @Test
    void targetFiresWhenPriceAtOrBelowTarget() {
        accept(product("P1", "\"10.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"10.00\""));

        alerts.createRule("P1", "STORE", AlertRuleEntity.Type.TARGET, new BigDecimal("10.00"));
        assertEquals(1, eventRepository.count());
    }

    @Test
    void targetDoesNotFireAboveTarget() {
        accept(product("P1", "\"12.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"12.00\""));

        alerts.createRule("P1", "STORE", AlertRuleEntity.Type.TARGET, new BigDecimal("10.00"));
        assertEquals(0, eventRepository.count());
    }

    @Test
    void dropFiresOnlyAgainstMostRecentDistinctPriorPrice() {
        accept(product("P1", "\"10.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"10.00\""));   // same price (new checksum) -> observation
        accept(product("P1", "\"8.00\""));    // drop

        alerts.createRule("P1", "STORE", AlertRuleEntity.Type.DROP, null);
        assertEquals(1, eventRepository.count());
    }

    @Test
    void dropWithIdenticalHistoryDoesNotFire() {
        accept(product("P1", "\"10.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"10.00\""));
        accept(product("P1", "\"10.00\""));

        alerts.createRule("P1", "STORE", AlertRuleEntity.Type.DROP, null);
        assertEquals(0, eventRepository.count());
    }

    @Test
    void repeatedImportsDoNotDuplicateEvents() {
        accept(product("P1", "\"9.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"9.00\""));
        alerts.createRule("P1", "STORE", AlertRuleEntity.Type.TARGET, new BigDecimal("10.00"));
        assertEquals(1, eventRepository.count());

        // Re-evaluating the same latest observation does not add a duplicate event.
        alerts.evaluateAll();
        assertEquals(1, eventRepository.count());
    }

    @Test
    void quietHoursSuppressVisibilityButNotStorage() {
        accept(product("P1", "\"9.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"9.00\""));

        CLOCK.set(Instant.parse("2026-11-10T15:00:00Z")); // 23:00 HKT, quiet
        alerts.createRule("P1", "STORE", AlertRuleEntity.Type.TARGET, new BigDecimal("10.00"));

        AlertPageView view = alerts.view();
        assertTrue(view.quietHours());
        assertEquals(1, eventRepository.count());
        assertFalse(view.events().getFirst().visible());
    }

    @Test
    void snoozeHidesEventForEightHours() {
        accept(product("P1", "\"9.00\""));
        tracking.enableTracking("P1");
        accept(product("P1", "\"9.00\""));
        alerts.createRule("P1", "STORE", AlertRuleEntity.Type.TARGET, new BigDecimal("10.00"));

        Long eventId = eventRepository.findAll().getFirst().getId();
        alerts.snooze(eventId);

        AlertPageView view = alerts.view();
        assertFalse(view.events().getFirst().visible());

        CLOCK.set(CLOCK.instant().plusSeconds(9 * 3600));
        alerts.evaluateAll();
        assertTrue(alerts.view().events().getFirst().visible());
    }

    private void accept(String... records) {
        catalog.accept(("[" + String.join(",", records) + "]").getBytes(StandardCharsets.UTF_8),
                "SAMPLE", "test");
    }

    private String product(String code, String storePrice) {
        return """
                {"code":"%s","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"%s","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat3Name":{"en":"Fresh","zh-Hant":"鮮奶","zh-Hans":"鲜奶"},
                "prices":[{"supermarketCode":"STORE","price":%s}],"offers":[]}
                """.formatted(code, code, storePrice).trim();
    }

    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override public ZoneId getZone() { return HK; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
