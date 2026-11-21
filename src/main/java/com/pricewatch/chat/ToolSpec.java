package com.pricewatch.chat;

import java.util.Map;

/** A tool definition advertised to the model (name, description, JSON-schema params). */
public record ToolSpec(String name, String description, Map<String, Object> parameters) {
}
