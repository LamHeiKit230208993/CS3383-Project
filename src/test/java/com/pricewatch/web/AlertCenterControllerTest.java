package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.importer.CatalogReplacementService;
import com.pricewatch.catalog.importer.ImportedCatalog;
import com.pricewatch.catalog.importer.ImportedCatalog.ImportedListing;
import com.pricewatch.catalog.importer.ImportedCatalog.ImportedProduct;
import com.pricewatch.notifications.ProductTargetService;
import com.pricewatch.notifications.WatchRuleService;
import com.pricewatch.notifications.WatchPriceKind;
import com.pricewatch.tracking.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:alert-center-http;DB_CLOSE_DELAY=-1",
        "spring.main.web-application-type=servlet",
        "pricewatch.jobs.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class AlertCenterControllerTest {
    private static final Instant NOW = Instant.parse("2026-11-22T00:00:00Z");
    private static final String MILK = "P000000124";
    private static final String BREAD = "P000000229";
    private static final String RULES = "/alerts/rules?code=" + MILK;
    private static final String CENTER = "/alerts?tab=rules";
    private static final String HISTORY = "/alerts";
    private static final String START = "2026-11-23T09:00";

    @Autowired private MockMvc mvc;
    @Autowired private CatalogService catalog;
    @Autowired private CatalogReplacementService replacement;
    @Autowired private TrackingService tracking;
    @Autowired private ProductTargetService targets;
    @Autowired private WatchRuleService watchRules;
    @MockitoBean private Clock clock;
    @MockitoBean private DeepSeekChatClient chatClient;

    @BeforeEach
    void prepareWishlistAndClock() {
        when(clock.instant()).thenReturn(NOW);
        catalog.importSample();
        tracking.save(MILK);
    }

    @Test
    void ruleEditorReturnsNotFoundForUnknownProductsAndReportsWishlistState() throws Exception {
        mvc.perform(get("/alerts/rules").param("code", "MISSING"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/alerts/rules").param("code", BREAD))
                .andExpect(status().isOk())
                .andExpect(model().attribute("wishlistTracking", false));
        mvc.perform(get("/alerts/rules").param("code", MILK).param("from", "saved"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("wishlistTracking", true))
                .andExpect(model().attribute("rulesFrom", "saved"));
    }

    @Test
    void legacyProductAndRetailerTargetsCanBeDisabledReenabledAndDeleted() throws Exception {
        action(post("/alerts/product-target").param("code", MILK).param("targetPrice", "-1"), RULES, "error");
        assertTrue(targets.rule(MILK).isEmpty());
        action(post("/alerts/product-target").param("code", MILK).param("targetPrice", " 12.50 "), RULES, "notice");
        assertEquals(0, new BigDecimal("12.50").compareTo(targets.target(MILK).orElseThrow()));
        var product = post("/alerts/product-target/enabled").param("code", MILK);
        action(product.param("enabled", "false").param("from", "center"), CENTER, "notice");
        assertFalse(targets.rule(MILK).orElseThrow().enabled());
        action(post("/alerts/product-target/enabled").param("code", MILK).param("enabled", "true")
                .param("from", "rules"), RULES + "&from=rules", "notice");
        assertTrue(targets.rule(MILK).orElseThrow().enabled());
        action(post("/alerts/product-target/delete").param("code", MILK).param("from", "saved"),
                RULES + "&from=saved", "notice");
        assertTrue(targets.rule(MILK).isEmpty());
        action(post("/alerts/product-target/enabled").param("code", MILK).param("enabled", "true"), RULES, "error");

        long retailer = aeonTrackingId();
        tracking.configureTarget(retailer, BigDecimal.TEN);
        String id = Long.toString(retailer);
        action(post("/alerts/retailer-rule/enabled").param("code", MILK).param("id", id)
                .param("enabled", "false").param("from", "center"), CENTER, "notice");
        assertFalse(tracking.legacyRules().getFirst().alertsEnabled());
        action(post("/alerts/retailer-rule/enabled").param("code", MILK).param("id", id)
                .param("enabled", "true"), RULES, "notice");
        assertTrue(tracking.legacyRules().getFirst().alertsEnabled());
        action(post("/alerts/retailer-rule/delete").param("code", MILK).param("id", id)
                .param("from", "other"), RULES, "notice");
        assertTrue(tracking.legacyRules().isEmpty());
        action(post("/alerts/retailer-rule/delete").param("code", MILK).param("id", "99999"), RULES, "error");
    }

    @Test
    void priceDropFormUsesDailyByDefaultAndKeepsTheSelectedKind() throws Exception {
        action(watchForm().param("condition", "price").param("priceKind", "DROP_PERCENT")
                .param("targetPrice", "25"), RULES, "notice");
        var rule = watchRules.rules(MILK).getFirst();
        assertEquals(WatchPriceKind.DROP_PERCENT, rule.priceKind());
        assertEquals("DAILY", rule.repeat());
        mvc.perform(get(RULES)).andExpect(status().isOk()).andExpect(content().string(
                org.hamcrest.Matchers.containsString("Drops by 25%")));
        action(watchForm().param("condition", "price").param("priceKind", "DROP_AMOUNT")
                .param("id", Long.toString(rule.id())).param("targetPrice", "3"), RULES, "notice");
        assertEquals(WatchPriceKind.DROP_AMOUNT, watchRules.rules(MILK).getFirst().priceKind());
    }

    @Test
    void watchRuleRoutesSaveEachSupportedTypeAndKeepRulesScopedToTheirProduct() throws Exception {
        action(watchForm().param("targetPrice", "10"), RULES, "notice");
        action(watchForm().param("condition", "legacy").param("targetPrice", "11").param("intervalDays", "7"),
                RULES, "notice");
        action(watchForm().param("condition", "price").param("targetPrice", "12").param("repeat", "ONCE"),
                RULES, "notice");
        action(watchForm().param("condition", "time").param("startAt", START).param("repeat", "WEEKLY"),
                RULES, "notice");
        action(watchForm().param("condition", "combined").param("targetPrice", "9")
                .param("startAt", START).param("repeat", "MONTHLY").param("from", "center"), CENTER, "notice");

        var rules = watchRules.rules(MILK);
        assertEquals(5, rules.size());
        assertEquals(7, rules.get(1).intervalDays());
        assertEquals("ONCE", rules.get(2).repeat());
        assertEquals("WEEKLY", rules.get(3).repeat());
        assertEquals("MONTHLY", rules.getLast().repeat());

        String id = Long.toString(rules.getLast().id());
        action(post("/alerts/watch-rules/enabled").param("code", BREAD).param("id", id).param("enabled", "false"),
                "/alerts/rules?code=" + BREAD, "error");
        assertTrue(watchRules.rules(MILK).getLast().enabled());
        action(post("/alerts/watch-rules/enabled").param("code", MILK).param("id", id).param("enabled", "false"),
                RULES, "notice");
        assertFalse(watchRules.rules(MILK).getLast().enabled());
        action(post("/alerts/watch-rules/enabled").param("code", MILK).param("id", id).param("enabled", "true"),
                RULES, "notice");
        assertTrue(watchRules.rules(MILK).getLast().enabled());
        action(post("/alerts/watch-rules/delete").param("code", MILK).param("id", id), RULES, "notice");
        action(post("/alerts/watch-rules/delete").param("code", MILK).param("id", id), RULES, "error");
        assertEquals(4, watchRules.rules(MILK).size());
    }

    @ParameterizedTest
    @MethodSource("invalidWatchForms")
    void malformedWatchRuleRequestsNeverSaveARecord(Map<String, String> parameters) throws Exception {
        var request = watchForm();
        parameters.forEach(request::param);
        action(request, RULES, "error");
        assertTrue(watchRules.rules(MILK).isEmpty());
    }

    static Stream<Arguments> invalidWatchForms() {
        return Stream.of(
                Arguments.of(Map.of("condition", "unknown")),
                Arguments.of(Map.of("condition", "price", "repeat", "ONCE")),
                Arguments.of(Map.of("condition", "time", "repeat", "ONCE")),
                Arguments.of(Map.of("condition", "time", "startAt", "not-a-date", "repeat", "ONCE")),
                Arguments.of(Map.of("condition", "legacy", "targetPrice", "10", "intervalDays", "soon")));
    }

    @Test
    void livePriceEventsCanBeReadAndSnoozedFromTheHistoryRoute() throws Exception {
        targets.setTarget(MILK, BigDecimal.TEN);
        long legacy = aeonTrackingId();
        tracking.configureTarget(legacy, BigDecimal.TEN);
        var label = new LocalizedText("Milk", "鮮奶", "鲜奶");
        var pack = new ImportedProduct(MILK, label, label, label, label, label,
                List.of(new ImportedListing("AEON", BigDecimal.ONE, List.of())));
        replacement.replace(new ImportedCatalog(List.of(pack), 1, 0), "LIVE",
                "fixture", NOW, "alert-center-live");
        long id = targets.events(NOW).getFirst().id();

        var response = mvc.perform(get("/alerts")).andExpect(status().isOk()).andReturn();
        var timeline = response.getModelAndView().getModel().get("timelineDays");
        assertTrue(((List<?>) timeline).stream().map(AlertCenterController.TimelineDay.class::cast)
                .flatMap(day -> day.entries().stream()).anyMatch(entry -> entry.price() != null));
        assertTrue(((List<?>) timeline).stream().map(AlertCenterController.TimelineDay.class::cast)
                .flatMap(day -> day.entries().stream()).anyMatch(entry -> entry.legacy() != null));

        action(post("/alerts/product-events/{id}/read", id), HISTORY, "notice");
        assertTrue(targets.events(NOW).getFirst().read());
        action(post("/alerts/product-events/{id}/snooze", id), HISTORY, "notice");
        assertFalse(targets.events(NOW).getFirst().available());
        action(post("/alerts/product-events/{id}/read", id + 10000), HISTORY, "error");
    }

    private MockHttpServletRequestBuilder watchForm() {
        return post("/alerts/watch-rules/save").param("code", MILK);
    }

    private long aeonTrackingId() {
        return tracking.trackingPeriods().stream()
                .filter(period -> MILK.equals(period.productCode()) && "AEON".equals(period.retailerCode()))
                .findFirst().orElseThrow().id();
    }

    private void action(MockHttpServletRequestBuilder request, String destination, String feedback) throws Exception {
        mvc.perform(request).andExpect(redirectedUrl(destination)).andExpect(flash().attributeExists(feedback));
    }
}
