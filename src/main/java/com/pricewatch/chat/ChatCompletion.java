package com.pricewatch.chat;

import java.util.List;

/** One assistant turn: optional text plus any tool calls it wants executed. */
public record ChatCompletion(String content, List<ToolCall> toolCalls) {
    public ChatCompletion {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public boolean wantsTools() {
        return !toolCalls.isEmpty();
    }

    public String text() {
        return content == null ? "" : content;
    }
}
