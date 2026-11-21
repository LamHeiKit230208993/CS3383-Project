package com.pricewatch.tracking;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceHistoryEntry(String retailerCode, Instant retrievedAt, BigDecimal price, long trackingId,
                                String sourceUrl) {}
