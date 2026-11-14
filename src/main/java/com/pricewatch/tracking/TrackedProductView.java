package com.pricewatch.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A saved product with its tracking state and retained observations. */
public record TrackedProductView(
        String productCode,
        String nameEn,
        String brandEn,
        boolean active,
        boolean saved,
        boolean tracked,
        List<String> trackedRetailers,
        List<ObservationView> observations) {
    public TrackedProductView {
        trackedRetailers = List.copyOf(trackedRetailers);
        observations = List.copyOf(observations);
    }

    public record ObservationView(String retailerCode, BigDecimal price, Instant retrievedAt,
                                  String offerSnapshot) {
    }
}
