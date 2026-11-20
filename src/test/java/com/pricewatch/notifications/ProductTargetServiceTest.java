package com.pricewatch.notifications;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.importer.CatalogReplacementService;
import com.pricewatch.catalog.importer.ImportedCatalog;
import com.pricewatch.catalog.importer.ImportedCatalog.ImportedListing;
import com.pricewatch.catalog.importer.ImportedCatalog.ImportedProduct;
import com.pricewatch.notifications.persistence.ProductTargetRetailerPrice;
import com.pricewatch.tracking.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:product-targets;DB_CLOSE_DELAY=-1",
        "pricewatch.jobs.enabled=false"
})
class ProductTargetServiceTest {
    private static final String PACK = "TARGET-PACK-1";
    private static final String OTHER_PACK = "TARGET-PACK-2";
    private static final LocalizedText LABEL = new LocalizedText("Milk", "牛奶", "牛奶");

    @Autowired private ProductTargetService targets;
    @Autowired private TrackingService tracking;
    @Autowired private CatalogService catalog;
    @Autowired private CatalogReplacementService replacement;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private Clock clock;
    private Instant now;

    @BeforeEach
    void startWithASavedPack() {
        now = Instant.parse("2026-11-19T03:00:00Z");
        when(clock.instant()).thenAnswer(call -> now);
        targets.reset();
        tracking.removeSaved(PACK);
        importPrices("SAMPLE", "24", "18", "15");
        tracking.save(PACK);
        targets.setTarget(PACK, new BigDecimal("10.00"));
    }

    @Test
    void groupsEveryQualifyingRetailerForTheExactPackAndRearmsOnlyAfterHigherKnownPrices() {
        importPrices("SAMPLE", "10", "8", null);
        importPrices("LIVE", null, null, null);
        assertTrue(targets.events(now).isEmpty());

        importPrices("LIVE", "10", "8", "15");
        var first = targets.events(now).getFirst();
        assertEquals(PACK, first.productCode());
        assertEquals(now, first.retrievedAt());
        assertEquals(List.of(new ProductTargetRetailerPrice("STORE-A", new BigDecimal("10.0000")),
                new ProductTargetRetailerPrice("STORE-B", new BigDecimal("8.0000"))), first.retailers());

        targets.setTarget(PACK, new BigDecimal("10"));
        importPrices("LIVE", "10", "8", "15");
        importPrices("LIVE", null, null, null);
        importPrices("LIVE", "9", "6", null);
        assertEquals(1, targets.events(now).size());
        importPrices("LIVE", "11", "13", null);
        importPrices("LIVE", "9", "6", null);
        var afterCrossing = targets.events(now);
        assertEquals(2, afterCrossing.size());
        assertEquals(first, afterCrossing.getLast(), "The original alert keeps its original evidence.");
    }

    @Test
    void removalPausesTheRuleAndActionsPreservePastAlerts() {
        assertThrows(IllegalArgumentException.class, () -> targets.setTarget(OTHER_PACK, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> targets.setTarget(PACK, new BigDecimal("1.00001")));
        tracking.removeSaved(PACK);
        importPrices("LIVE", "5", "8", null);
        assertEquals(new BigDecimal("10.0000"), targets.target(PACK).orElseThrow());
        assertTrue(targets.events(now).isEmpty());

        tracking.save(PACK);
        importPrices("LIVE", "5", "8", null);
        long id = targets.events(now).getFirst().id();
        targets.markRead(id);
        targets.snooze(id);
        assertTrue(targets.events(now).getFirst().read());
        assertFalse(targets.events(now).getFirst().available());
        assertTrue(targets.events(now.plus(Duration.ofHours(24))).getFirst().available());
        targets.deleteTarget(PACK);
        assertTrue(targets.target(PACK).isEmpty());
        assertEquals(id, targets.events(now).getFirst().id());
        targets.reset();
        assertTrue(targets.events(now).isEmpty());
        assertEquals(0L, jdbc.queryForObject("select count(*) from product_target_event_prices", Long.class));
    }

    @Test
    void switchPausesAlertEvaluationAndKeepsTheTargetAndHistory() {
        targets.setEnabled(PACK, false);
        importPrices("LIVE", "5", "8", null);
        assertTrue(targets.events(now).isEmpty());
        assertFalse(targets.rule(PACK).orElseThrow().enabled());
        assertEquals(List.of(new ProductTargetService.RuleView(PACK, new BigDecimal("10.0000"), false)),
                targets.rules());
        assertFalse(tracking.priceHistory(PACK).isEmpty());

        tracking.removeSaved(PACK);
        targets.setTarget(PACK, new BigDecimal("9"));
        assertFalse(targets.rule(PACK).orElseThrow().enabled());
        tracking.save(PACK);
        targets.setEnabled(PACK, true);
        importPrices("LIVE", "5", "8", null);
        assertEquals(new BigDecimal("9.0000"), targets.events(now).getFirst().target());

        targets.setEnabled(PACK, false);
        importPrices("LIVE", "6", "7", null);
        assertEquals(1, targets.events(now).size());
    }

    @Test
    void failedCatalogueAcceptanceRollsBackTheEventAndLeavesTheTargetArmed() {
        var earlier = catalog.status();
        jdbc.execute("alter table accepted_import add constraint ck_product_target_rollback check (checksum <> 'ROLLBACK')");
        try {
            assertThrows(DataIntegrityViolationException.class, () -> replacement.replace(
                    snapshot("6", "15", null), "LIVE", "fixture", now, "ROLLBACK"));
            assertEquals(earlier, catalog.status());
            assertTrue(targets.events(now).isEmpty());
        } finally {
            jdbc.execute("alter table accepted_import drop constraint ck_product_target_rollback");
        }
        importPrices("LIVE", "6", "15", null);
        assertEquals(1, targets.events(now).size());
    }

    private void importPrices(String origin, String first, String second, String third) {
        now = now.plusSeconds(60);
        replacement.replace(snapshot(first, second, third), origin, "fixture", now, now.toString());
    }

    private static ImportedCatalog snapshot(String first, String second, String third) {
        return new ImportedCatalog(List.of(
                product(PACK, List.of(listing("STORE-A", first), listing("STORE-B", second), listing("STORE-C", third))),
                product(OTHER_PACK, List.of(listing("STORE-A", "1")))), 4, 0);
    }

    private static ImportedProduct product(String code, List<ImportedListing> listings) {
        return new ImportedProduct(code, LABEL, LABEL, LABEL, LABEL, LABEL, listings);
    }

    private static ImportedListing listing(String retailer, String price) {
        return new ImportedListing(retailer, price == null ? null : new BigDecimal(price), List.of());
    }
}
