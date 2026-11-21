package com.pricewatch.chat;

import java.util.List;

/**
 * One chat message in an OpenAI-compatible conversation.
 * For tool results use {@link #tool(String, String)}; for assistant tool requests
 * the {@code toolCalls} list is non-empty.
 */
public record ChatMessage(String role, String content, List<ToolCall> toolCalls) {
    public ChatMessage {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, List.of());
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, List.of());
    }

    public static ChatMessage assistant(String content, List<ToolCall> toolCalls) {
        return new ChatMessage("assistant", content, toolCalls);
    }

    /** A tool result; {@code toolCallId} links it back to the request. */
    public static ChatMessage tool(String toolCallId, String content) {
        return new ChatMessage("tool:" + toolCallId, content, List.of());
    }

    public String toolCallId() {
        return role.startsWith("tool:") ? role.substring("tool:".length()) : null;
    }
}
