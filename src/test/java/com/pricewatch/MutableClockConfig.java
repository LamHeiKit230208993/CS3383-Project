package com.pricewatch;

import com.pricewatch.config.ClockConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;

/** Test clock that tests can advance (D05: controlled time). */
@TestConfiguration
public class MutableClockConfig {
    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2026-10-31T08:00:00Z"), ClockConfig.ZONE);
    }

    public static class MutableClock extends Clock {
        private Instant instant;
        private final java.time.ZoneId zone;

        public MutableClock(Instant instant, java.time.ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        public void set(Instant instant) { this.instant = instant; }
        public void advance(java.time.Duration duration) { this.instant = this.instant.plus(duration); }

        @Override public java.time.ZoneId getZone() { return zone; }
        @Override public Clock withZone(java.time.ZoneId zone) { return new MutableClock(instant, zone); }
        @Override public Instant instant() { return instant; }
    }
}
