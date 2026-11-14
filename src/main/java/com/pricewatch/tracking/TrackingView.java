package com.pricewatch.tracking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TrackingView(long id, String productCode, String productName, String retailerCode,
                           boolean enabled, Instant startedAt, Instant stoppedAt, BigDecimal targetPrice,
                           boolean dropEnabled, List<PriceObservationView> observations) {
    public TrackingView {
        observations = List.copyOf(observations);
    }
}
