package com.pricewatch.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * J12: flash notices, error text, and page titles in the active UI language.
 * Controllers use this instead of hardcoded English so a language switch also
 * covers the feedback shown after a form submit.
 */
@Component
public class UiMessages {
    private final MessageSource messages;

    public UiMessages(MessageSource messages) {
        this.messages = messages;
    }

    /** Message for the request's LANG cookie; the key must exist in messages*.properties. */
    public String get(HttpServletRequest request, String key, Object... args) {
        return messages.getMessage(key, args, LocaleController.currentLocale(request));
    }

    /**
     * Text for an error thrown by a service. A {@link com.pricewatch.UiTextException}
     * carries its own key, so the same rule shows in the user's language; any other
     * exception keeps its own message.
     */
    public String error(HttpServletRequest request, RuntimeException exception) {
        if (exception instanceof com.pricewatch.UiTextException localized) {
            return messages.getMessage(localized.key(), localized.args(),
                    LocaleController.currentLocale(request));
        }
        return exception.getMessage();
    }
}
