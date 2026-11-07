package com.pricewatch.alerts;

import com.pricewatch.MutableClockConfig;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.persistence.ProductEntity;
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
import com.pricewatch.alerts.persistence.AlertRuleEntity.Type;
import com.pricewatch.alerts.repository.AlertEventRepository;
import com.pricewatch.alerts.repository.AlertRuleRepository;
import com.pricewatch.alerts.repository.BudgetSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MutableClockConfig.class)
class AlertServiceTest {
    @Autowired private CatalogService catalog;
    @Autowired private TrackingService tracking;
    @Autowired private AlertService alerts;
    @Autowired private MutableClockConfig.MutableClock clock;
    @Autowired private AlertRuleRepository rules;
    @Autowired private AlertEventRepository events;
    @Autowired private PriceObservationRepository observations;
    @Autowired private TrackingChoiceRepository choices;
    @Autowired private WishlistEntryRepository wishlist;
    @Autowired private BudgetSettingsRepository budgetSettings;
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
        events.deleteAllInBatch();
        rules.deleteAllInBatch();
        budgetSettings.deleteAllInBatch();
        observations.deleteAllInBatch();
        choices.deleteAllInBatch();
        wishlist.deleteAllInBatch();
        importRepository.deleteAllInBatch();
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        clock.set(Instant.parse("2026-10-31T08:00:00Z")); // 16:00 HKT, outside quiet hours
    }

    @Test
    void targetFiresFromOneKnownObservation() {
        setupTracked();
        alerts.createRule("P000000124", Type.TARGET, new BigDecimal("8.00"));
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"7.50\"}]"));
        assertEquals(1, events.count());
        var event = events.findAll().get(0);
        assertEquals(Type.TARGET, event.getType());
        assertEquals(new BigDecimal("7.5000"), event.getPrice());
    }

    @Test
    void dropNeedsAValidEarlierKnownPair() {
        setupTracked();
        alerts.createRule("P000000124", Type.DROP, null);
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"9.00\"}]"));
        assertEquals(0, events.count(), "no earlier price yet");

        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"8.00\"}]"));
        assertEquals(1, events.count(), "drop from 9.00 to 8.00");
        assertEquals(new BigDecimal("9.0000"), events.findAll().get(0).getComparedPrice());

        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"8.00\"}]"));
        assertEquals(1, events.count(), "no duplicate event for the same price");
    }

    @Test
    void unknownObservationNeverFires() {
        setupTracked();
        alerts.createRule("P000000124", Type.DROP, null);
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"9.00\"}]"));
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":null}]"));
        assertEquals(0, events.count(), "unknown price cannot fire a drop");
    }

    @Test
    void quietHoursHoldEventsUntilMorningShow() {
        setupTracked();
        clock.set(Instant.parse("2026-10-31T15:00:00Z")); // 23:00 HKT quiet
        alerts.createRule("P000000124", Type.TARGET, new BigDecimal("8.00"));
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"7.50\"}]"));
        assertEquals(1, events.count());
        assertTrue(alerts.inQuietHours());
        assertTrue(alerts.heldEvents().size() >= 1);
        assertEquals(0, alerts.showHeldNow(), "held during quiet hours");
        assertFalse(events.findAll().get(0).isVisible());

        clock.set(Instant.parse("2026-10-31T23:30:00Z")); // 07:30 HKT next day
        int shown = alerts.showHeldNow();
        assertTrue(shown >= 1);
        assertTrue(alerts.morningSummary().stream().anyMatch(e -> e.getPrice()
                .compareTo(new BigDecimal("7.5000")) == 0));
    }

    @Test
    void snoozeHidesEventFor24Hours() {
        setupTracked();
        alerts.createRule("P000000124", Type.TARGET, new BigDecimal("8.00"));
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"7.50\"}]"));
        Long eventId = events.findAll().get(0).getId();
        alerts.snooze(eventId);
        assertTrue(events.findById(eventId).orElseThrow().getSnoozeUntil().isAfter(clock.instant()));
        clock.advance(Duration.ofHours(1));
        assertEquals(0, alerts.showHeldNow(), "still snoozed");
        clock.advance(Duration.ofHours(24));
        assertTrue(alerts.showHeldNow() >= 1, "visible after snooze expires");
    }

    @Test
    void updateRuleChangesTypeAndAmountAndListsForProduct() {
        setupTracked();
        var rule = alerts.createRule("P000000124", Type.TARGET, new BigDecimal("8.00"));
        assertEquals(1, alerts.rulesForProduct("P000000124").size());

        alerts.updateRule(rule.getId(), Type.DROP, null);
        assertEquals(Type.DROP, rules.findById(rule.getId()).orElseThrow().getType());
        assertNull(rules.findById(rule.getId()).orElseThrow().getTargetAmount());

        alerts.disableRule(rule.getId());
        assertTrue(alerts.rulesForProduct("P000000124").isEmpty());
    }

    @Test
    void eventViewsShowTheRuleThatFired() {
        setupTracked();
        alerts.createRule("P000000124", Type.TARGET, new BigDecimal("8.00"));
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"7.50\"}]"));

        var view = alerts.eventViews().get(0);
        assertEquals("AEON", view.event().getRetailerCode());
        assertNotNull(view.rule());
        assertEquals(new BigDecimal("8.0000"), view.rule().getTargetAmount());
    }

    @Test
    void targetFiresFromLowestAcrossRetailersAndNamesThatRetailer() {
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"12.00\"},"
                + "{\"supermarketCode\":\"WELLCOME\",\"price\":\"9.00\"},"
                + "{\"supermarketCode\":\"JASONS\",\"price\":\"11.00\"}]"));
        tracking.enableTracking("P000000124");
        alerts.createRule("P000000124", Type.TARGET, new BigDecimal("10.00"));

        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"12.00\"},"
                + "{\"supermarketCode\":\"WELLCOME\",\"price\":\"9.50\"},"
                + "{\"supermarketCode\":\"JASONS\",\"price\":\"11.00\"}]"));

        assertEquals(1, events.count(), "one product-wide target event");
        var event = events.findAll().get(0);
        assertEquals("WELLCOME", event.getRetailerCode(), "named the lowest retailer");
        assertEquals(new BigDecimal("9.5000"), event.getPrice());
    }

    @Test
    void dropFiresWhenAnyRetailerDrops() {
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"10.00\"},"
                + "{\"supermarketCode\":\"WELLCOME\",\"price\":\"10.00\"}]"));
        tracking.enableTracking("P000000124");
        alerts.createRule("P000000124", Type.DROP, null);
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"10.00\"},"
                + "{\"supermarketCode\":\"WELLCOME\",\"price\":\"10.00\"}]"));
        assertEquals(0, events.count(), "no drop yet");

        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"10.00\"},"
                + "{\"supermarketCode\":\"WELLCOME\",\"price\":\"8.00\"}]"));
        assertEquals(1, events.count(), "wellcome dropped");
        assertEquals("WELLCOME", events.findAll().get(0).getRetailerCode());

        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"10.00\"},"
                + "{\"supermarketCode\":\"WELLCOME\",\"price\":\"8.00\"}]"));
        assertEquals(1, events.count(), "no duplicate for same retailer/price");
    }

    private void setupTracked() {
        accept(product("[{\"supermarketCode\":\"AEON\",\"price\":\"10.00\"}]"));
        tracking.enableTracking("P000000124");
    }

    private void accept(String... records) {
        catalog.accept(("[" + String.join(",", records) + "]").getBytes(StandardCharsets.UTF_8), "SAMPLE", "test");
    }

    private String product(String prices) {
        return """
                {"code":"P000000124","brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                "name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                "cat2Name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                "cat3Name":{"en":"Fresh","zh-Hant":"鮮奶","zh-Hans":"鲜奶"},
                "prices":%s,"offers":[]}
                """.formatted(prices).trim();
    }
}
