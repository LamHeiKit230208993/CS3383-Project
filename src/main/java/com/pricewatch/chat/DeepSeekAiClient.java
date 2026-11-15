package com.pricewatch.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek-compatible chat client with OpenAI-style tool calling. The base URL and
 * key come from the app's own configuration ({@code pricewatch.ai.*}); they are
 * never hardcoded, logged, or committed.
 */
@Component
public class DeepSeekAiClient implements AiClient {
    private static final int TIMEOUT_MILLIS = 30_000;
    private static final int MAX_RESPONSE_BYTES = 1 * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final URI endpoint;
    private final String apiKey;
    private final String model;

    public DeepSeekAiClient(@Value("${pricewatch.ai.base-url:}") String baseUrl,
                            @Value("${pricewatch.ai.api-key:}") String apiKey,
                            @Value("${pricewatch.ai.model:CS3383/DeepSeek-V4-Flash}") String model) {
        this.endpoint = baseUrl == null || baseUrl.isBlank() ? null : chatCompletions(baseUrl.strip());
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.model = model;
    }

    @Override
    public boolean isConfigured() {
        return endpoint != null && !apiKey.isEmpty();
    }

    @Override
    public ChatCompletion completeTurn(List<ChatMessage> messages, List<ToolSpec> tools) {
        if (!isConfigured()) {
            throw new AiUnavailableException("The AI assistant is not configured.");
        }
        HttpURLConnection connection = null;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", 0);
            body.put("stream", false);
            body.put("messages", toWireMessages(messages));
            if (tools != null && !tools.isEmpty()) {
                body.put("tools", toWireTools(tools));
                body.put("tool_choice", "auto");
            }
            URLConnection opened = endpoint.toURL().openConnection();
            if (!(opened instanceof HttpURLConnection http)) {
                throw new AiUnavailableException("The AI base URL must use HTTP or HTTPS.");
            }
            connection = http;
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            try (java.io.OutputStream out = connection.getOutputStream()) {
                out.write(JSON.writeValueAsBytes(body));
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new AiUnavailableException("The AI provider returned HTTP " + status + ".");
            }
            byte[] bytes = connection.getInputStream().readNBytes(MAX_RESPONSE_BYTES + 1);
            if (bytes.length > MAX_RESPONSE_BYTES) {
                throw new AiUnavailableException("The AI response was too large.");
            }
            return parse(JSON.readTree(bytes));
        } catch (AiUnavailableException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new AiUnavailableException("Could not reach the AI provider.", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private List<Map<String, Object>> toWireMessages(List<ChatMessage> messages) {
        List<Map<String, Object>> wire = new ArrayList<>();
        for (ChatMessage message : messages) {
            Map<String, Object> entry = new LinkedHashMap<>();
            if (message.toolCallId() != null) {
                entry.put("role", "tool");
                entry.put("tool_call_id", message.toolCallId());
                entry.put("content", message.content());
            } else {
                entry.put("role", message.role());
                if (message.content() != null) {
                    entry.put("content", message.content());
                }
                if (!message.toolCalls().isEmpty()) {
                    entry.put("tool_calls", message.toolCalls().stream().map(call -> Map.of(
                            "id", call.id(),
                            "type", "function",
                            "function", Map.of("name", call.name(), "arguments", call.argumentsJson())
                    )).toList());
                }
            }
            wire.add(entry);
        }
        return wire;
    }

    private static List<Map<String, Object>> toWireTools(List<ToolSpec> tools) {
        return tools.stream().map(tool -> Map.of(
                "type", "function",
                "function", Map.of("name", tool.name(), "description", tool.description(),
                        "parameters", tool.parameters())
        )).toList();
    }

    private ChatCompletion parse(JsonNode root) {
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");
        String content = message.path("content").isTextual() ? message.path("content").asText() : "";
        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode call : message.path("tool_calls")) {
            calls.add(new ToolCall(call.path("id").asText(),
                    call.path("function").path("name").asText(),
                    call.path("function").path("arguments").asText("{}")));
        }
        if (calls.isEmpty() && content.isBlank()) {
            throw new AiUnavailableException("The AI provider returned no answer.");
        }
        return new ChatCompletion(content, calls);
    }

    /** Appends the OpenAI-compatible path unless the base URL already names it. */
    static URI chatCompletions(String baseUrl) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (trimmed.endsWith("/chat/completions")) {
            return URI.create(trimmed);
        }
        return URI.create(trimmed + "/chat/completions");
    }
}
