package com.pricewatch.chat;

/** AI provider was not reachable, refused, or returned an unusable answer. */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiUnavailableException(String message) {
        super(message);
    }
}
