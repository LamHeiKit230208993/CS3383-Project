package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.tracking.PriceHistoryEntry;
import com.pricewatch.tracking.TrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceHistoryChartTest {
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    @Test
    void splitsARetailerLineAtUnknownPricesAndTrackingRestarts() {
        var chart = PriceHistoryChart.from(List.of(
                entry("STORE-A", 0, "8.00", 102),
                entry("STORE-A", 1, "5.00", 102),
                entry("STORE-A", 2, "10.00", 101),
                entry("STORE-B", 3, "8.00", 201),
                entry("STORE-A", 3, null, 101),
                entry("STORE-A", 4, "9.00", 101),
                entry("STORE-A", 5, "7.00", 101))).orElseThrow();

        assertEquals(List.of("STORE-A", "STORE-B"), chart.series().stream()
                .map(PriceHistoryChart.Series::retailer).toList());
        assertEquals(List.of(2, 1, 2), chart.series().getFirst().segments().stream()
                .map(segment -> segment.points().size()).toList());
        assertEquals("12,127 135,50", chart.series().getFirst().segments().getFirst().coordinates());
        assertEquals(new BigDecimal("5.00"), chart.minimum());
        assertEquals(new BigDecimal("10.00"), chart.maximum());
    }

    @Test
    void handlesOneKnownPriceAndAnUnknownOnlyView() {
        var known = entry("STORE-A", 0, "7.00", 1);
        var chart = PriceHistoryChart.from(List.of(known)).orElseThrow();
        assertEquals("320,108", chart.series().getFirst().segments().getFirst().coordinates());
        assertEquals(chart.first(), chart.last());
        assertTrue(PriceHistoryChart.from(List.of(entry("STORE-A", 0, null, 1))).isEmpty());
    }

    @Test
    void filtersTheTableAndChartTogetherWhileKeepingAllRetailersSelectable() {
        var catalog = mock(CatalogService.class);
        var tracking = mock(TrackingService.class);
        var product = mock(ProductView.class);
        when(catalog.find("PACK")).thenReturn(product);
        var chosen = entry("STORE-B", 7, "12.00", 2);
        when(tracking.priceHistory("PACK")).thenReturn(List.of(
                entry("STORE-A", 2, "8.00", 1),
                chosen,
                entry("STORE-B", 35, "11.00", 2),
                entry("STORE-C", 40, "9.00", 3)));
        var controller = new PriceHistoryController(catalog, tracking, mock(HelloController.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
        var model = new ExtendedModelMap();

        assertEquals("market-desk", controller.history("PACK", 30, "STORE-B", model));
        assertEquals(List.of(chosen), model.get("observations"));
        assertEquals(List.of("STORE-A", "STORE-B", "STORE-C"), model.get("retailers"));
        assertEquals("STORE-B", ((PriceHistoryChart) model.get("chart")).series().getFirst().retailer());
        assertEquals(30, model.get("selectedDays"));
    }

    private PriceHistoryEntry entry(String retailer, int daysAgo, String price, long period) {
        return new PriceHistoryEntry(retailer, NOW.minus(daysAgo, ChronoUnit.DAYS),
                price == null ? null : new BigDecimal(price), period);
    }
}
