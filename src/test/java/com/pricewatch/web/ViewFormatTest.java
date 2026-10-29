package com.pricewatch.web;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewFormatTest {
    private final ViewFormat format = new ViewFormat(ZoneId.of("Asia/Hong_Kong"));

    @Test
    void keepsUnknownFourDecimalAndHongKongFormatting() {
        assertEquals("Not listed", format.money(null));
        assertTrue(format.money(new BigDecimal("1234.5000")).contains("1,234.50"));
        assertEquals("29 Oct 2026, 16:24 HKT",
                format.time(Instant.parse("2026-10-29T08:24:00Z")));
    }
}
