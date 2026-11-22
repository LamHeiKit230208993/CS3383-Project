package com.pricewatch.web;

import com.pricewatch.preferences.DisplayPreferencesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@ControllerAdvice(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
class DisplayLocaleResolver implements LocaleResolver {
    private static final String REQUEST_KEY = DisplayLocaleResolver.class.getName() + ".preferences";
    private final DisplayPreferencesService preferences;

    DisplayLocaleResolver(DisplayPreferencesService preferences) {
        this.preferences = preferences;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return Locale.forLanguageTag(displayPreferences(request).language());
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("Change the locale through display preferences");
    }

    @ModelAttribute("displayPreferences")
    public DisplayPreferencesService.View displayPreferences(HttpServletRequest request) {
        if (request.getAttribute(REQUEST_KEY) instanceof DisplayPreferencesService.View saved) return saved;
        var current = preferences.current();
        request.setAttribute(REQUEST_KEY, current);
        return current;
    }
}
