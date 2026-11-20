package com.pricewatch.web;

import com.pricewatch.tracking.PriceHistoryEntry;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record PriceHistoryChart(Instant first, Instant last, BigDecimal minimum, BigDecimal maximum,
                                List<Series> series) {
    private static final List<String> LINE_STYLES = List.of("none", "9 4", "2 4", "9 4 2 4", "15 5", "5 3");

    static Optional<PriceHistoryChart> from(List<PriceHistoryEntry> newestFirst) {
        var prices = newestFirst.stream().map(PriceHistoryEntry::price).filter(price -> price != null).toList();
        if (prices.isEmpty()) return Optional.empty();

        var first = newestFirst.getLast().retrievedAt();
        var last = newestFirst.getFirst().retrievedAt();
        var min = Collections.min(prices);
        var max = Collections.max(prices);
        Map<String, List<PriceHistoryEntry>> byRetailer = new TreeMap<>();
        for (PriceHistoryEntry entry : newestFirst.reversed()) {
            byRetailer.computeIfAbsent(entry.retailerCode(), ignored -> new ArrayList<>()).add(entry);
        }
        var series = new ArrayList<Series>();
        for (var retailer : byRetailer.entrySet()) {
            var segments = segments(retailer.getValue(), first, last, min, max);
            if (segments.isEmpty()) continue;
            int palette = series.size() % LINE_STYLES.size();
            series.add(new Series(retailer.getKey(), palette, LINE_STYLES.get(palette), segments));
        }
        return Optional.of(new PriceHistoryChart(first, last, min, max, List.copyOf(series)));
    }

    private static List<Segment> segments(List<PriceHistoryEntry> entries, Instant first, Instant last,
                                         BigDecimal min, BigDecimal max) {
        var segments = new ArrayList<Segment>();
        var points = new ArrayList<Point>();
        long period = -1;
        for (var entry : entries) {
            if (!points.isEmpty() && (entry.price() == null || entry.trackingId() != period)) {
                segments.add(new Segment(List.copyOf(points)));
                points.clear();
            }
            period = entry.trackingId();
            if (entry.price() == null) continue;
            var priceSpan = max.subtract(min);
            double yFraction = priceSpan.signum() == 0 ? 0.5
                    : entry.price().subtract(min).divide(priceSpan, MathContext.DECIMAL64).doubleValue();
            points.add(new Point(dateX(entry.retrievedAt(), first, last),
                    Math.round(204 - 192 * yFraction), entry.retrievedAt()));
        }
        if (!points.isEmpty()) segments.add(new Segment(List.copyOf(points)));
        return List.copyOf(segments);
    }

    public long dateX(Instant observedAt) {
        return dateX(observedAt, first, last);
    }

    private static long dateX(Instant observedAt, Instant first, Instant last) {
        double duration = last.toEpochMilli() - first.toEpochMilli();
        return duration == 0 ? 320 : Math.round(12 + 616 * (observedAt.toEpochMilli() - first.toEpochMilli()) / duration);
    }

    public record Point(long x, long y, Instant observedAt) {}
    public record Segment(List<Point> points) {
        public String coordinates() {
            return points.stream().map(point -> point.x() + "," + point.y()).collect(Collectors.joining(" "));
        }
    }
    public record Series(String retailer, int palette, String dash, List<Segment> segments) {}
}
