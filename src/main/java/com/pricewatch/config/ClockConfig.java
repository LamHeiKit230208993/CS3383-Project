package com.pricewatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Single application clock (D05). Asia/Hong_Kong matches the HKT labels used by
 * the web formatters. Tests replace this bean with a fixed clock.
 */
@Configuration
public class ClockConfig {
    public static final ZoneId ZONE = ZoneId.of("Asia/Hong_Kong");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
