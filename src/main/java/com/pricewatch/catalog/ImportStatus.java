package com.pricewatch.catalog;

import java.time.Instant;
import java.time.LocalDate;

public record ImportStatus(
        String origin,
        String sourceUrl,
        Instant retrievedAt,
        LocalDate sourceDate,
        String checksum,
        int productCount,
        int listingCount,
        int offerCount) {
}
