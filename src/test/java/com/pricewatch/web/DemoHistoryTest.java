package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.tracking.DemoHistoryService;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.TrackingView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** J12: the Price data page can seed demo products so history, alerts, and tracking can be tested. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demodata;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.web-application-type=servlet",
        "pricewatch.jobs.enabled=false"})
@AutoConfigureMockMvc
class DemoHistoryTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogService catalog;
    @Autowired TrackingService tracking;
    @Autowired HistoryService history;
    @Autowired DemoHistoryService demo;
    @Autowired PersonalDataService personalData;
    @Autowired ProductRepository productRepository;
    @Autowired AlertEventRepository alertEventRepository;
    @MockitoBean Clock appClock;

    private final AtomicReference<java.time.Instant> now =
            new AtomicReference<>(java.time.Instant.parse("2026-11-18T04:00:00Z"));

    @BeforeEach
    void fixedClock() {
        when(appClock.instant()).thenAnswer(invocation -> now.get());
        // Each case starts from empty user data so seeded rows do not add up.
        personalData.deleteLocalData();
    }

    @Test
    void dataPageOffersTheDemoAction() throws Exception {
        mvc.perform(get("/data")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Load demo history data")))
                .andExpect(content().string(containsString("Load demo data")))
                .andExpect(content().string(containsString("synthetic prices")));
    }

    @Test
    void seedCreatesDatedHistoryWithGapsAndKeepsTheRestOfTheList() throws Exception {
        catalog.importSample();
        var result = demo.seed();

        assertEquals(3, result.productCount());
        assertTrue(result.observationCount() >= 20, "each demo series gets its dated rows");
        assertTrue(catalog.catalogue().stream().allMatch(product -> product.code().startsWith("DEMO-")),
                "the accepted catalogue is now only the demo products");

        var products = history.products();
        assertEquals(3, products.stream().map(HistoryService.ProductHistory::code).toList().size());
        var milkAeon = products.stream()
                .filter(product -> product.code().equals("DEMO-MILK"))
                .flatMap(product -> product.retailers().stream())
                .filter(retailer -> retailer.retailerCode().equals("AEON"))
                .findFirst().orElseThrow();
        assertEquals(8, milkAeon.observationCount(), "one recorded row per demo day");
        assertTrue(milkAeon.rows().size() > milkAeon.observationCount(),
                "gap rows are added on top of the recorded days");
        assertTrue(milkAeon.rows().stream().anyMatch(HistoryService.Row::isGap),
                "the skipped demo days appear as a visible gap");
        assertTrue(milkAeon.rows().stream()
                        .filter(row -> !row.isGap())
                        .allMatch(row -> DemoHistoryService.DEMO_SOURCE.equals(row.sourceUrl())),
                "demo rows are labelled so they are not mistaken for live prices");

        var coffee = products.stream()
                .filter(product -> product.code().equals("DEMO-COFFEE"))
                .flatMap(product -> product.retailers().stream())
                .filter(retailer -> retailer.retailerCode().equals("WELLCOME"))
                .findFirst().orElseThrow();
        assertTrue(coffee.rows().stream().anyMatch(row -> !row.isGap() && row.price() == null),
                "the missing demo price stays unknown");
    }

    @Test
    void seedStartsProductWideTrackingAndOneTargetAlert() throws Exception {
        demo.seed();
        List<TrackingService.AllRetailersView> allRetailers = tracking.allRetailersProducts();
        assertEquals(3, allRetailers.size());
        assertTrue(allRetailers.stream().allMatch(product -> product.code().startsWith("DEMO-")));
        assertTrue(tracking.trackingPeriods().stream().anyMatch(TrackingView::enabled),
                "tracking is on for the demo products");
        assertTrue(alertEventRepository.count() > 0, "the falling demo prices create a target or drop alert");
        assertEquals(3, catalog.catalogue().size(), "only demo products are in the accepted catalogue");
        assertNotNull(catalog.status());
    }

    @Test
    void demoActionNeedsConfirmationWhenACatalogueExists() throws Exception {
        catalog.importSample();
        mvc.perform(post("/data/demo")).andExpect(redirectedUrl("/data"))
                .andExpect(flash().attribute("error", containsString("Confirm")));
        assertTrue(catalog.catalogue().stream().anyMatch(product -> "P000000124".equals(product.code())),
                "an unconfirmed demo load keeps the current catalogue");

        mvc.perform(post("/data/demo").param("confirmSample", "true"))
                .andExpect(redirectedUrl("/history"));
        assertTrue(catalog.catalogue().stream().noneMatch(product -> "P000000124".equals(product.code())),
                "a confirmed demo load replaces the catalogue");
    }
}
