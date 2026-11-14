package com.pricewatch.tracking;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceObservationView(BigDecimal price, Instant observedAt, String sourceUrl) {
}
