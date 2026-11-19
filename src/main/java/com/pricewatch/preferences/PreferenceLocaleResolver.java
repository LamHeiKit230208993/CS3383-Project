package com.pricewatch.preferences;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/** The one local user's saved language applies to every request and page. */
@Component("localeResolver")
public class PreferenceLocaleResolver implements LocaleResolver {
    private final PreferenceService preferences;

    public PreferenceLocaleResolver(PreferenceService preferences) {
        this.preferences = preferences;
    }

    public static final String REQUEST_LOCALE_ATTRIBUTE = "pricewatch.locale";

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Locale locale = preferences.locale();
        request.setAttribute(REQUEST_LOCALE_ATTRIBUTE, locale);
        return locale;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        // Language changes use the normal /preferences form so they persist without JavaScript.
    }
}
