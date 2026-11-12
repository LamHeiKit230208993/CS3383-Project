package com.pricewatch;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppClockConfiguration {
    @Bean
    Clock appClock() {
        return Clock.systemUTC();
    }
}
