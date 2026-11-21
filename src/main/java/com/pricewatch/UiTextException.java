package com.pricewatch;

/**
 * J12: a user-facing error whose text is a message key, so the controllers can
 * show it in the active UI language. {@link #getMessage()} keeps the English
 * wording for tests and logs that read the exception directly.
 */
public class UiTextException extends IllegalArgumentException {
    private final String key;
    private final Object[] args;

    public UiTextException(String key, String englishMessage, Object... args) {
        super(englishMessage);
        this.key = key;
        this.args = args;
    }

    public String key() {
        return key;
    }

    public Object[] args() {
        return args.clone();
    }
}
