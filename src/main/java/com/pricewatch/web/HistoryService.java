package com.pricewatch.web;

import com.pricewatch.tracking.PriceObservationView;
import com.pricewatch.tracking.TrackingView;
import com.pricewatch.tracking.TrackingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * J12 R10/R14: present only the dated price records that exist. When recorded
 * days are missing, a visible gap row marks it; no daily series is invented.
 */
@Service
public class HistoryService {
    private static final ZoneId HONG_KONG = ZoneId.of("Asia/Hong_Kong");
    private final TrackingService tracking;

    public HistoryService(TrackingService tracking) {
        this.tracking = tracking;
    }

    public List<ProductHistory> products() {
        Map<String, List<TrackingView>> byProduct = new LinkedHashMap<>();
        for (var period : tracking.trackingPeriods()) {
            byProduct.computeIfAbsent(period.productCode(), code -> new ArrayList<>()).add(period);
        }
        List<ProductHistory> result = new ArrayList<>();
        for (var entry : byProduct.entrySet()) {
            var first = entry.getValue().getFirst();
            List<RetailerHistory> retailers = entry.getValue().stream()
                    .map(period -> new RetailerHistory(period.retailerCode(), period.id(),
                            rowise(period.observations())))
                    .toList();
            result.add(new ProductHistory(entry.getKey(), first.productName(), retailers));
        }
        return List.copyOf(result);
    }

    /** One row per stored observation plus a gap row where recorded days are missing. */
    static List<Row> rowise(List<PriceObservationView> observations) {
        List<PriceObservationView> ascending = new ArrayList<>(observations);
        ascending.sort((a, b) -> a.observedAt().compareTo(b.observedAt()));
        List<Row> rows = new ArrayList<>();
        LocalDate previous = null;
        for (PriceObservationView observation : ascending) {
            LocalDate day = observation.observedAt().atZone(HONG_KONG).toLocalDate();
            if (previous != null && day.isAfter(previous.plusDays(1))) {
                rows.add(new Row(null, null, null, previous.plusDays(1), day.minusDays(1)));
            }
            rows.add(new Row(observation.observedAt(), observation.price(), observation.sourceUrl(), null, null));
            previous = day;
        }
        return List.copyOf(rows);
    }

    public record ProductHistory(String code, String name, List<RetailerHistory> retailers) {
    }

    public record RetailerHistory(String retailerCode, long trackingId, List<Row> rows) {
        public boolean isEmpty() {
            return rows.isEmpty();
        }

        public long observationCount() {
            return rows.stream().filter(row -> !row.isGap()).count();
        }
    }

    /** A stored record, or a gap between two recorded days. */
    public record Row(Instant observedAt, BigDecimal price, String sourceUrl,
                      LocalDate gapFrom, LocalDate gapTo) {
        public boolean isGap() {
            return gapFrom != null;
        }
    }
}
