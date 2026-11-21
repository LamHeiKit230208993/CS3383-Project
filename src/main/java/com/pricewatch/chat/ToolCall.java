package com.pricewatch.chat;

/** A tool call requested by the model. */
public record ToolCall(String id, String name, String argumentsJson) {
}
