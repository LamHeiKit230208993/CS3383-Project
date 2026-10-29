package com.pricewatch.catalog;

import com.pricewatch.support.FixedClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:clock-test;DB_CLOSE_DELAY=-1")
@Import(FixedClockConfiguration.class)
class CatalogClockTest {
    @Autowired
    private CatalogService catalog;

    @Test
    void importUsesTheInjectedClock() {
        ImportStatus accepted = catalog.importSample();

        assertEquals(FixedClockConfiguration.FIXED_INSTANT, accepted.retrievedAt());
        assertEquals(FixedClockConfiguration.FIXED_INSTANT, catalog.status().retrievedAt());
    }
}
