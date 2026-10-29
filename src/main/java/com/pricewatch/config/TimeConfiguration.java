package com.pricewatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfiguration {

    @Bean
    public ZoneId pricewatchZone(@Value("${pricewatch.zone-id:Asia/Hong_Kong}") String zoneId) {
        return ZoneId.of(zoneId);
    }

    @Bean
    public Clock clock(ZoneId pricewatchZone) {
        return Clock.system(pricewatchZone);
    }
}
