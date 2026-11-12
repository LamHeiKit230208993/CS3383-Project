package com.pricewatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * One clock for alerts, summaries, and reminders. The zone matches the source data
 * (Hong Kong). Tests replace this bean with a fixed clock.
 */
@Configuration
public class ClockConfig {
    public static final ZoneId ZONE = ZoneId.of("Asia/Hong_Kong");

    @Bean
    public Clock appClock() {
        return Clock.system(ZONE);
    }
}
