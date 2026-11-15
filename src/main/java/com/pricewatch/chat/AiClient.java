package com.pricewatch.chat;

import java.util.List;

/**
 * Sends chat messages to the AI provider, supporting OpenAI-style tool calls.
 * Implementations send the configured key server-side only.
 */
public interface AiClient {
    /** True when the provider base URL and key are configured. */
    boolean isConfigured();

    /**
     * One turn. Returns the assistant message, which may contain tool calls.
     * Throws {@link AiUnavailableException} when the provider cannot answer.
     *
     * @param messages  full conversation so far (roles: system, user, assistant, tool)
     * @param tools     tool definitions to advertise, or null for plain completion
     */
    ChatCompletion completeTurn(List<ChatMessage> messages, List<ToolSpec> tools);
}
