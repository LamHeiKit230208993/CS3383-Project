package com.pricewatch.notifications;

import com.pricewatch.PricewatchApplication;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.importer.CatalogReplacementService;
import com.pricewatch.catalog.importer.ImportedCatalog;
import com.pricewatch.catalog.importer.ImportedCatalog.ImportedListing;
import com.pricewatch.catalog.importer.ImportedCatalog.ImportedProduct;
import com.pricewatch.tracking.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:watch-rules;DB_CLOSE_DELAY=-1",
        "pricewatch.jobs.enabled=false"
})
class WatchRuleServiceTest {
    private static final String PACK = "WATCH-PACK";
    private static final String OTHER_PACK = "OTHER-PACK";
    private static final Instant START = Instant.parse("2026-11-20T03:00:00Z");
    private static final LocalizedText LABEL = new LocalizedText("Milk", "牛奶", "牛奶");

    @Autowired private WatchRuleService rules;
    @Autowired private ProductTargetService alerts;
    @Autowired private TrackingService tracking;
    @Autowired private CatalogReplacementService replacement;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private Clock clock;
    private Instant now;

    @BeforeEach
    void prepareSavedPack() {
        now = START;
        when(clock.instant()).thenAnswer(call -> now);
        rules.reset();
        alerts.reset();
        tracking.removeSaved(PACK);
        accept("SAMPLE", "20", "25");
        tracking.save(PACK);
    }

    @Test
    void liveDropsRespectExactThresholdsAndKeepEvidencePerRetailer() {
        rules.save(PACK, null, new BigDecimal("3"), null, null, WatchPriceKind.DROP_AMOUNT);
        rules.save(PACK, null, new BigDecimal("25"), null, null, WatchPriceKind.DROP_PERCENT);
        assertTrue(rules.rules(PACK).stream().allMatch(rule -> "DAILY".equals(rule.repeat())));
        accept("LIVE", "12", "8");
        assertTrue(alerts.events(now).isEmpty());
        accept("LIVE", "9", "6");
        var events = alerts.events(now);
        assertEquals(2, events.size());
        var percent = events.stream().filter(event -> event.priceKind() == WatchPriceKind.DROP_PERCENT)
                .findFirst().orElseThrow();
        assertEquals(List.of("STORE-A", "STORE-B"), percent.retailers().stream().map(p -> p.retailerCode()).toList());
        assertEquals(new BigDecimal("12.0000"), percent.retailers().getFirst().previousPrice());
        assertEquals(new BigDecimal("9.0000"), percent.retailers().getFirst().price());
        assertTrue(rules.rules(PACK).stream().allMatch(WatchRuleView::enabled));
    }

    @Test
    void dropRulesRequireConsecutiveLiveListingsAndValidateTheThreshold() {
        assertThrows(IllegalArgumentException.class, () -> rules.save(PACK, null, BigDecimal.ZERO,
                null, null, WatchPriceKind.DROP_AMOUNT));
        assertThrows(IllegalArgumentException.class, () -> rules.save(PACK, null, new BigDecimal("101"),
                null, null, WatchPriceKind.DROP_PERCENT));
        rules.save(PACK, null, BigDecimal.TEN, null, "ONCE", WatchPriceKind.DROP_PERCENT);
        accept("LIVE", "0", null);
        accept("LIVE", "0", "3");
        accept("SAMPLE", "10", "10");
        accept("LIVE", "1", null);
        assertTrue(alerts.events(now).isEmpty());
        accept("LIVE", "0.90", "1");
        assertEquals(1, alerts.events(now).size());
        assertTrue(rules.rules(PACK).getFirst().completed());
    }

    @Test
    void independentTargetsKeepTheirOwnArmingAndHistoricEvidence() {
        var ten = addRule("10", null);
        var fifteen = addRule("15", null);
        assertThrows(IllegalArgumentException.class, () -> rules.delete(OTHER_PACK, ten.id()));
        assertThrows(IllegalArgumentException.class, () -> rules.save(PACK, ten.id(), BigDecimal.TEN, 7));
        assertThrows(IllegalArgumentException.class, () -> rules.save(PACK, null, BigDecimal.TEN, 366));
        assertThrows(IllegalArgumentException.class, () -> rules.save(OTHER_PACK, null, BigDecimal.TEN, null));

        accept("LIVE", "13", "12");
        var first = alerts.events(now).getFirst();
        assertEquals(fifteen.id(), first.ruleId());
        assertNull(first.scheduledDueAt());
        assertEquals(List.of("STORE-A", "STORE-B"), first.retailers().stream().map(price -> price.retailerCode()).toList());

        accept("LIVE", "9", null);
        rules.save(PACK, fifteen.id(), new BigDecimal("15.0"), null);
        accept("LIVE", null, null);
        accept("LIVE", "8", "7");
        assertEquals(2, alerts.events(now).size());
        assertEquals(ten.id(), alerts.events(now).getFirst().ruleId());
        assertFalse(rule(fifteen.id()).armed());

        accept("LIVE", "20", null);
        accept("LIVE", "9", "14");
        assertEquals(4, alerts.events(now).size());
        rules.delete(PACK, fifteen.id());
        assertEquals(first, alerts.events(now).getLast());
    }

    @Test
    void combinedOccurrenceRemainsPendingUntilAQualifyingLiveRefresh() {
        var combined = addRule("10", 7);
        assertFalse(combined.waitingForLive());
        accept("LIVE", "8", "9");
        rules.checkDue();
        assertTrue(alerts.events(now).isEmpty());

        accept("LIVE", "12", null);
        now = combined.nextDueAt();
        // A threshold edit can make the cached price qualify. The due occurrence still waits for LIVE.
        rules.save(PACK, combined.id(), new BigDecimal("13"), 7);
        rules.checkDue();
        accept("SAMPLE", "2", "3");
        rules.checkDue();
        accept("LIVE", null, null);
        assertTrue(alerts.events(now).isEmpty());
        assertEquals(combined.nextDueAt(), rule(combined.id()).nextDueAt());
        assertTrue(rule(combined.id()).waitingForLive());

        now = now.plus(2, ChronoUnit.DAYS);
        accept("LIVE", "12", "13");
        var fired = alerts.events(now).getFirst();
        assertEquals(combined.id(), fired.ruleId());
        assertEquals(combined.nextDueAt(), fired.scheduledDueAt());
        assertEquals(now, fired.createdAt());
        assertEquals(2, fired.retailers().size());
        assertEquals(now.plus(7, ChronoUnit.DAYS), rule(combined.id()).nextDueAt());
        assertFalse(rule(combined.id()).waitingForLive());
        accept("LIVE", "12", "13");
        rules.checkDue();
        assertEquals(1, alerts.events(now).size());

        rules.save(PACK, combined.id(), new BigDecimal("15"), 3);
        assertEquals(now.plus(3, ChronoUnit.DAYS), rule(combined.id()).nextDueAt());
        Instant retrieved = now;
        now = now.plus(3, ChronoUnit.DAYS);
        rules.checkDue();
        assertEquals(2, alerts.events(now).size());
        assertEquals(retrieved, alerts.events(now).getFirst().retrievedAt());
    }

    @Test
    void removingAnItemPausesItsCombinedRuleAndFailedImportsDoNotAdvanceIt() {
        var combined = addRule("10", 1);
        now = combined.nextDueAt();
        tracking.removeSaved(PACK);
        accept("LIVE", "8", "9");
        rules.checkDue();
        assertTrue(alerts.events(now).isEmpty());
        assertEquals(now, rule(combined.id()).nextDueAt());

        tracking.save(PACK);
        accept("LIVE", "20", null);
        rules.checkDue();
        jdbc.execute("alter table accepted_import add constraint ck_watch_rule_rollback check (checksum <> 'ROLLBACK')");
        try {
            assertThrows(DataIntegrityViolationException.class, () -> replacement.replace(
                    snapshot("7", "8"), "LIVE", "fixture", now, "ROLLBACK"));
            assertTrue(alerts.events(now).isEmpty());
            assertEquals(now, rule(combined.id()).nextDueAt());
        } finally {
            jdbc.execute("alter table accepted_import drop constraint ck_watch_rule_rollback");
        }
        accept("LIVE", "7", "8");
        assertEquals(1, alerts.events(now).size());
        alerts.markRead(alerts.events(now).getFirst().id());
        rules.delete(PACK, combined.id());
        assertTrue(alerts.events(now).getFirst().read());
        assertTrue(rules.rules(PACK).isEmpty());
    }

    @Test
    void aNewlyDueOccurrenceCannotUseCheapSampleData() {
        var rule = addRule("10", 1);
        now = rule.nextDueAt();
        accept("SAMPLE", "7", "8");
        rules.checkDue();
        assertTrue(alerts.events(now).isEmpty());
        assertTrue(rule(rule.id()).waitingForLive());

        accept("LIVE", "7", "8");
        assertEquals(rule.nextDueAt(), alerts.events(now).getFirst().scheduledDueAt());
    }

    @Test
    void scheduledCheckAndLiveRefreshClaimOnlyOneOccurrence() throws Exception {
        var combined = addRule("10", 1);
        accept("LIVE", "7", "8");
        now = combined.nextDueAt();
        var start = new CountDownLatch(1);
        try (var tasks = Executors.newFixedThreadPool(2)) {
            var due = tasks.submit(() -> {
                start.await();
                rules.checkDue();
                return null;
            });
            var refresh = tasks.submit(() -> {
                start.await();
                accept("LIVE", "8", "9");
                return null;
            });
            start.countDown();
            due.get(10, TimeUnit.SECONDS);
            refresh.get(10, TimeUnit.SECONDS);
        }
        assertEquals(1, alerts.events(now).size());
        assertEquals(combined.nextDueAt(), alerts.events(now).getFirst().scheduledDueAt());
        assertEquals(now.plus(1, ChronoUnit.DAYS), rule(combined.id()).nextDueAt());
    }

    @Test
    void oncePriceRuleRunsOnLiveMarketDataAndStaysOffWhenCompleted() {
        rules.save(PACK, null, BigDecimal.TEN, null, "ONCE");
        long id = rules.rules(PACK).getFirst().id();
        rules.setEnabled(PACK, id, false);
        accept("LIVE", "7", "8");
        assertTrue(alerts.events(now).isEmpty());
        assertFalse(rule(id).enabled());

        rules.setEnabled(PACK, id, true);
        accept("SAMPLE", "7", "8");
        assertTrue(alerts.events(now).isEmpty());
        accept("LIVE", "7", "8");
        assertEquals(1, alerts.events(now).size());
        assertEquals(now, alerts.events(now).getFirst().retrievedAt());
        assertTrue(rule(id).completed());
        assertFalse(rule(id).enabled());
        assertThrows(IllegalArgumentException.class, () -> rules.setEnabled(PACK, id, true));

        now = now.plus(1, ChronoUnit.DAYS);
        accept("LIVE", "6", "8");
        assertEquals(1, alerts.events(now).size());
        rules.save(PACK, id, new BigDecimal("10.00"), null, "ONCE");
        assertTrue(rule(id).completed());
        rules.save(PACK, id, new BigDecimal("9"), null, "ONCE");
        assertTrue(rule(id).enabled());
        accept("LIVE", "6", "8");
        assertEquals(2, alerts.events(now).size());
    }

    @Test
    void recurringPriceOnlyWaitsForTheNextPeriodAndFreshLiveRefresh() {
        rules.save(PACK, null, BigDecimal.TEN, null, "DAILY");
        long id = rules.rules(PACK).getFirst().id();
        assertThrows(IllegalArgumentException.class, () -> rules.save(PACK, null, null, null, "DAILY"));
        assertThrows(IllegalArgumentException.class, () -> rules.save(PACK, null, BigDecimal.TEN, null, "MINUTELY"));
        accept("LIVE", "7", "8");
        assertEquals(1, alerts.events(now).size());
        assertNull(rule(id).nextDueAt());

        now = WatchRepeat.nextHongKongDay(START).minusSeconds(1);
        accept("LIVE", "20", null);
        accept("LIVE", "9", "7");
        assertEquals(1, alerts.events(now).size());
        now = WatchRepeat.nextHongKongDay(START);
        accept("SAMPLE", "2", "3");
        rules.checkDue();
        assertEquals(1, alerts.events(now).size());
        accept("LIVE", "10", "9");
        accept("LIVE", "9", "8");
        assertEquals(2, alerts.events(now).size());
        assertTrue(rule(id).enabled());
    }

    @Test
    void aCalendarOnlyRuleCanBePausedAndConcurrentChecksRecordItOnce() throws Exception {
        LocalDateTime start = LocalDateTime.parse("2026-11-20T12:00");
        rules.save(OTHER_PACK, null, null, start, "ONCE");
        var created = rules.rules(OTHER_PACK).getFirst();
        assertEquals(Instant.parse("2026-11-20T04:00:00Z"), created.nextDueAt());
        assertEquals(start, created.startAt());
        rules.setEnabled(OTHER_PACK, created.id(), false);
        now = created.nextDueAt();
        rules.checkDue();
        assertTrue(alerts.events(now).isEmpty());

        rules.setEnabled(OTHER_PACK, created.id(), true);
        var startTogether = new CountDownLatch(1);
        try (var tasks = Executors.newFixedThreadPool(2)) {
            Callable<Void> check = () -> {
                startTogether.await();
                rules.checkDue();
                return null;
            };
            var first = tasks.submit(check);
            var second = tasks.submit(check);
            startTogether.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        var occurrences = alerts.events(now);
        assertEquals(1, occurrences.size());
        var event = occurrences.getFirst();
        assertEquals(OTHER_PACK, event.productCode());
        assertEquals(created.id(), event.ruleId());
        assertEquals(now, event.scheduledDueAt());
        assertNull(event.target());
        assertNull(event.retrievedAt());
        assertTrue(event.retailers().isEmpty());
        var fired = rules.rules(OTHER_PACK).getFirst();
        assertFalse(fired.enabled());
        assertTrue(fired.completed());
        assertNull(fired.nextDueAt());
    }

    @Test
    void calendarRepeatsKeepHongKongTimeAndOriginalMonthDay() {
        LocalDateTime start = LocalDateTime.parse("2027-01-31T10:00");
        rules.save(PACK, null, null, start, "MONTHLY");
        long id = rules.rules(PACK).getFirst().id();
        tracking.removeSaved(PACK);
        now = Instant.parse("2027-01-31T02:00:00Z");
        rules.checkDue();
        assertEquals(Instant.parse("2027-02-28T02:00:00Z"), rule(id).nextDueAt());

        now = Instant.parse("2027-03-01T02:00:00Z");
        rules.checkDue();
        assertEquals(2, alerts.events(now).size());
        assertEquals(Instant.parse("2027-02-28T02:00:00Z"), alerts.events(now).getFirst().scheduledDueAt());
        assertEquals(Instant.parse("2027-03-31T02:00:00Z"), rule(id).nextDueAt());

        now = Instant.parse("2027-05-01T02:00:00Z");
        rules.checkDue();
        assertEquals(3, alerts.events(now).size());
        assertEquals(Instant.parse("2027-05-31T02:00:00Z"), rule(id).nextDueAt());

        rules.save(PACK, null, null, LocalDateTime.parse("2027-05-01T10:00"), "FORTNIGHTLY");
        long fortnightly = rules.rules(PACK).getLast().id();
        rules.checkDue();
        assertEquals(Instant.parse("2027-05-15T02:00:00Z"), rule(fortnightly).nextDueAt());
    }

    @Test
    void scheduledCombinedTargetWaitsForFreshLiveDataEvenWhenCachedPricesQualify() {
        var start = LocalDateTime.parse("2026-11-20T12:00");
        rules.save(PACK, null, BigDecimal.TEN, start, "ONCE");
        accept("LIVE", "8", "9");
        now = WatchRepeat.inHongKong(start);
        rules.checkDue();
        assertTrue(alerts.events(now).isEmpty());
        assertTrue(rules.rules(PACK).getFirst().waitingForLive());
        accept("LIVE", "8", "9");
        assertEquals(1, alerts.events(now).size());
        assertTrue(rules.rules(PACK).getFirst().completed());
    }

    @Test
    void combinedCalendarRuleWaitsAfterAMissedCheckAndKeepsItsWeeklyAnchor() {
        LocalDateTime start = LocalDateTime.parse("2026-11-20T12:00");
        rules.save(PACK, null, BigDecimal.TEN, start, "WEEKLY");
        var combined = rules.rules(PACK).getFirst();
        accept("LIVE", "12", null);
        now = combined.nextDueAt();
        rules.checkDue();
        assertTrue(rule(combined.id()).waitingForLive());
        rules.save(PACK, combined.id(), new BigDecimal("13"), start, "WEEKLY");
        rules.checkDue();
        assertTrue(alerts.events(now).isEmpty());

        now = combined.nextDueAt().plus(2, ChronoUnit.DAYS);
        accept("LIVE", "13", "11");
        var event = alerts.events(now).getFirst();
        assertEquals(combined.id(), event.ruleId());
        assertEquals(combined.nextDueAt(), event.scheduledDueAt());
        assertEquals(now, event.retrievedAt());
        assertEquals(2, event.retailers().size());
        assertEquals(combined.nextDueAt().plus(7, ChronoUnit.DAYS), rule(combined.id()).nextDueAt());
        accept("LIVE", "8", "9");
        assertEquals(1, alerts.events(now).size());
    }

    @Test
    void databaseUpgradeKeepsLegacyTargetsAndEvidence(@TempDir Path directory) {
        String url = "jdbc:h2:file:" + directory.resolve("legacy-alerts").toAbsolutePath()
                .toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
        long oldEventId;
        try (var before = openDatabase(url)) {
            var catalogue = before.getBean(CatalogReplacementService.class);
            catalogue.replace(snapshot("20", "30"), "SAMPLE", "fixture", START, "SAMPLE");
            before.getBean(TrackingService.class).save(PACK);
            var legacy = before.getBean(ProductTargetService.class);
            legacy.setTarget(PACK, BigDecimal.TEN);
            catalogue.replace(snapshot("9", "8"), "LIVE", "fixture", START, "LIVE");
            var event = legacy.events(Instant.MAX).getFirst();
            oldEventId = event.id();
            legacy.markRead(oldEventId);
            var sql = before.getBean(JdbcTemplate.class);
            sql.execute("drop table watch_rules");
            sql.execute("alter table product_target_events drop column rule_id");
            sql.execute("alter table product_target_events drop column scheduled_due_at");
        }
        try (var upgraded = openDatabase(url)) {
            var legacy = upgraded.getBean(ProductTargetService.class);
            assertEquals(0, legacy.target(PACK).orElseThrow().compareTo(BigDecimal.TEN));
            var original = legacy.events(Instant.MAX).getFirst();
            assertEquals(oldEventId, original.id());
            assertTrue(original.read());
            assertEquals(2, original.retailers().size());
            assertNull(original.ruleId());
            assertNull(original.scheduledDueAt());
            var upgradedRules = upgraded.getBean(WatchRuleService.class);
            upgradedRules.save(PACK, null, BigDecimal.TEN, null);
            upgraded.getBean(CatalogReplacementService.class).replace(
                    snapshot("9", "8"), "LIVE", "fixture", START.plusSeconds(1), "UPDATED");
            assertEquals(upgradedRules.rules(PACK).getFirst().id(), legacy.events(Instant.MAX).getFirst().ruleId());
            assertEquals(original, legacy.events(Instant.MAX).getLast());
        }
    }

    @Test
    void fileUpgradeRelaxesOldRequiredColumnsAndRetainsExistingRuleAndEvent(@TempDir Path directory) {
        String url = "jdbc:h2:file:" + directory.resolve("required-watch-columns").toAbsolutePath()
                .toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE";
        long oldId;
        try (var before = openDatabase(url)) {
            var oldCatalog = before.getBean(CatalogReplacementService.class);
            oldCatalog.replace(snapshot("20", "30"), "SAMPLE", "fixture", START, "SAMPLE");
            before.getBean(TrackingService.class).save(PACK);
            var oldRules = before.getBean(WatchRuleService.class);
            oldRules.save(PACK, null, BigDecimal.TEN, null);
            oldId = oldRules.rules(PACK).getFirst().id();
            oldCatalog.replace(snapshot("7", "8"), "LIVE", "fixture", START, "LIVE");
            var sql = before.getBean(JdbcTemplate.class);
            sql.execute("alter table watch_rules alter column target set not null");
            sql.execute("alter table product_target_events alter column target set not null");
            sql.execute("alter table product_target_events alter column retrieved_at set not null");
            for (String column : List.of("paused", "repeat_frequency", "schedule_start_at", "completed_at",
                    "price_anchor_at", "next_price_at")) {
                sql.execute("alter table watch_rules drop column " + column);
            }
        }
        try (var upgraded = openDatabase(url)) {
            var upgradedRules = upgraded.getBean(WatchRuleService.class);
            var oldRule = upgradedRules.rules(PACK).getFirst();
            assertEquals(oldId, oldRule.id());
            assertTrue(oldRule.enabled());
            assertFalse(oldRule.armed());
            assertNull(oldRule.repeat());
            var history = upgraded.getBean(ProductTargetService.class);
            var existing = history.events(Instant.MAX).getFirst();
            assertEquals(oldId, existing.ruleId());
            assertEquals(2, existing.retailers().size());

            upgradedRules.save(OTHER_PACK, null, null, LocalDateTime.parse("2000-01-01T10:00"), "ONCE");
            upgradedRules.checkDue();
            assertEquals(2, history.events(Instant.MAX).size());
            var calendar = history.events(Instant.MAX).getFirst();
            assertEquals(OTHER_PACK, calendar.productCode());
            assertNull(calendar.target());
            assertNull(calendar.retrievedAt());
            assertEquals(existing, history.events(Instant.MAX).getLast());
        }
    }

    private static ConfigurableApplicationContext openDatabase(String url) {
        return new SpringApplicationBuilder(PricewatchApplication.class).web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + url, "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.main.banner-mode=off", "--pricewatch.jobs.enabled=false");
    }

    @Test
    void editingADueCalendarTargetUsesTheNewThresholdOnTheNextLiveRefresh() {
        now = START.plus(1, ChronoUnit.HOURS);
        accept("LIVE", "9", "12");
        Instant due = START.plus(1, ChronoUnit.DAYS);
        LocalDateTime startAt = LocalDateTime.ofInstant(due, java.time.ZoneId.of("Asia/Hong_Kong"));
        rules.save(PACK, null, new BigDecimal("10"), startAt, "WEEKLY");
        var original = rules.rules(PACK).getLast();
        assertTrue(alerts.events(now).isEmpty());

        now = due;
        rules.save(PACK, original.id(), new BigDecimal("8"), startAt, "WEEKLY");
        assertTrue(alerts.events(now).isEmpty());
        accept("LIVE", "9", "12");
        assertTrue(alerts.events(now).isEmpty());
        accept("LIVE", "8", "12");

        var event = alerts.events(now).getFirst();
        assertEquals(original.id(), event.ruleId());
        assertEquals(due, event.scheduledDueAt());
        assertEquals(0, new BigDecimal("8").compareTo(event.target()));
        assertEquals(List.of("STORE-A"),
                event.retailers().stream().map(price -> price.retailerCode()).toList());
        assertEquals(0, new BigDecimal("8").compareTo(rule(original.id()).target()));
        assertEquals(due.plus(7, ChronoUnit.DAYS), rule(original.id()).nextDueAt());
    }

    @Test
    void aTimeOnlyRuleForAnUnwishlistedProductCannotBeChangedIntoAPriceTarget() {
        LocalDateTime startAt = LocalDateTime.ofInstant(START.plus(1, ChronoUnit.DAYS),
                java.time.ZoneId.of("Asia/Hong_Kong"));
        rules.save(OTHER_PACK, null, null, startAt, "ONCE");
        var timeOnly = rules.rules(OTHER_PACK).getLast();
        assertThrows(IllegalArgumentException.class,
                () -> rules.save(OTHER_PACK, timeOnly.id(), BigDecimal.TEN, startAt, "ONCE"));
        assertEquals(timeOnly, rules.rules(OTHER_PACK).getLast());
        rules.save(PACK, null, BigDecimal.TEN, null, null);
        assertEquals("DAILY", rules.rules(PACK).getLast().repeat());
    }

    @Test
    void acceptedRefreshSkipsPausedTargetsAndTimeOnlyRules() {
        var target = addRule("10", null);
        LocalDateTime startAt = LocalDateTime.ofInstant(START.plus(1, ChronoUnit.DAYS),
                java.time.ZoneId.of("Asia/Hong_Kong"));
        rules.save(PACK, null, null, startAt, "ONCE");
        var timeOnly = rules.rules(PACK).getLast();
        rules.setEnabled(PACK, target.id(), false);

        now = START.plus(1, ChronoUnit.HOURS);
        accept("LIVE", "9", "12");
        assertTrue(alerts.events(now).isEmpty());
        assertEquals(timeOnly.nextDueAt(), rule(timeOnly.id()).nextDueAt());

        rules.setEnabled(PACK, target.id(), true);
        now = now.plus(1, ChronoUnit.HOURS);
        accept("LIVE", "9", "12");
        assertEquals(List.of(target.id()), alerts.events(now).stream().map(ProductTargetEventView::ruleId).toList());
    }

    private WatchRuleView addRule(String target, Integer intervalDays) {
        rules.save(PACK, null, new BigDecimal(target), intervalDays);
        return rules.rules(PACK).getLast();
    }

    private WatchRuleView rule(long id) {
        return rules.rules(PACK).stream().filter(rule -> rule.id() == id).findFirst().orElseThrow();
    }

    private void accept(String origin, String first, String second) {
        replacement.replace(snapshot(first, second), origin, "fixture", now, origin + "-" + now);
    }

    private static ImportedCatalog snapshot(String first, String second) {
        return new ImportedCatalog(List.of(
                product(PACK, List.of(listing("STORE-A", first), listing("STORE-B", second))),
                product(OTHER_PACK, List.of(listing("STORE-A", "1")))), 3, 0);
    }

    private static ImportedProduct product(String code, List<ImportedListing> listings) {
        return new ImportedProduct(code, LABEL, LABEL, LABEL, LABEL, LABEL, listings);
    }

    private static ImportedListing listing(String retailer, String price) {
        return new ImportedListing(retailer, price == null ? null : new BigDecimal(price), List.of());
    }
}
