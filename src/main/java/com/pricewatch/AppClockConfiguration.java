package com.pricewatch;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

@Configuration
@EnableScheduling
public class AppClockConfiguration {
    @Bean
    Clock appClock() {
        return Clock.systemUTC();
    }

    /**
     * J12: the UI language follows the LANG cookie set by /lang (en, zh_TW,
     * zh_CN); anything else falls back to English. No session is used, so the
     * choice survives a restart.
     */
    @Bean(name = "localeResolver")
    CookieLocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("LANG");
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(60 * 60 * 24 * 365);
        resolver.setCookiePath("/");
        resolver.setLanguageTagCompliant(true);
        resolver.setDefaultLocaleFunction(AppClockConfiguration::normalized);
        return resolver;
    }

    static Locale normalized(HttpServletRequest request) {
        return Locale.ENGLISH;
    }
}
