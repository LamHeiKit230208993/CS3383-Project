package com.pricewatch.i18n;

import com.pricewatch.preferences.PreferenceLocaleResolver;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Message lookup for services that do not receive a request-bound MessageSource.
 * Explicit bundle selection keeps the saved language independent of the host
 * operating system's default locale, and keeps standalone unit tests on English.
 */
public final class AppMessages {
    private static final ResourceBundle.Control NO_SYSTEM_FALLBACK = new ResourceBundle.Control() {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return null;
        }
    };

    private AppMessages() {
    }

    private static Locale requestLocale() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object resolved = attributes.getAttribute(PreferenceLocaleResolver.REQUEST_LOCALE_ATTRIBUTE,
                    RequestAttributes.SCOPE_REQUEST);
            if (resolved instanceof Locale locale) return locale;
        }
        return Locale.ENGLISH;
    }

    public static String text(String key, Object... arguments) {
        try {
            String pattern = ResourceBundle.getBundle("messages", requestLocale(), NO_SYSTEM_FALLBACK)
                    .getString(key);
            return arguments.length == 0 ? pattern : MessageFormat.format(pattern, arguments);
        } catch (MissingResourceException exception) {
            return key;
        }
    }
}
