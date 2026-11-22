package com.pricewatch.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Component
class DeepSeekChatClient {
    private static final Logger LOG = LoggerFactory.getLogger(DeepSeekChatClient.class);
    private static final Duration TURN_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_TOOL_ROUNDS = 3;
    private static final int MAX_TOOL_CALLS = 8;
    private static final int MAX_ARGUMENT_CHARS = 4_096;
    private static final int MAX_RESPONSE_CHARS = 65_536;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final JsonMapper json = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private final URI endpoint;
    private final String key;
    private final String model;

    DeepSeekChatClient(@Value("${pricewatch.chat.base-url:https://llm.cs.cityu.edu.hk/v1}") String url,
                       @Value("${DEEPSEEK_API_KEY_SKU:}") String key,
                       @Value("${pricewatch.chat.model:CS3383/DeepSeek-V4-Flash}") String model) {
        endpoint = URI.create(url.replaceFirst("/+$", "") + "/chat/completions");
        this.key = key;
        this.model = model;
    }

    Optional<String> reply(String systemPrompt, List<ChatMessage> history, List<Tool> tools,
                           BiFunction<String, JsonNode, Object> executeTool) {
        if (key.isBlank()) {
            LOG.debug("Chat provider API key is missing");
            return Optional.empty();
        }

        long deadline = System.nanoTime() + TURN_TIMEOUT.toNanos();
        var body = json.createObjectNode().put("model", model).put("max_tokens", 768);
        body.putObject("thinking").put("type", "disabled");
        if (!tools.isEmpty()) {
            var definitions = body.putArray("tools");
            for (Tool tool : tools) {
                definitions.addObject().put("type", "function").set("function", json.valueToTree(tool));
            }
            body.put("tool_choice", "auto");
        }
        var messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        for (ChatMessage message : history) {
            messages.addObject().put("role", message.role()).put("content", message.content());
        }
        var toolNames = tools.stream().map(Tool::name).collect(Collectors.toSet());
        var seenIds = new HashSet<String>();

        try {
            for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
                JsonNode choice = complete(body, deadline);
                JsonNode message = choice.path("message");
                String finishReason = choice.path("finish_reason").asText();
                if (!"assistant".equals(message.path("role").asText())
                        || "length".equals(finishReason) || "content_filter".equals(finishReason)) {
                    return Optional.empty();
                }
                JsonNode calls = message.path("tool_calls");
                if (calls.isMissingNode() || calls.isNull() || (calls.isArray() && calls.isEmpty())) {
                    if ("tool_calls".equals(finishReason)) return Optional.empty();
                    String content = message.path("content").textValue();
                    return Optional.ofNullable(content).map(String::strip).filter(text -> !text.isEmpty());
                }
                if (round == MAX_TOOL_ROUNDS || !calls.isArray() || seenIds.size() + calls.size() > MAX_TOOL_CALLS) {
                    return Optional.empty();
                }
                for (JsonNode call : calls) {
                    String id = call.path("id").textValue();
                    JsonNode function = call.path("function");
                    if (id == null || id.isBlank() || id.length() > 128 || !seenIds.add(id)
                            || !"function".equals(call.path("type").asText())
                            || !function.path("name").isTextual() || !function.path("arguments").isTextual()) {
                        return Optional.empty();
                    }
                }

                var assistant = messages.addObject().put("role", "assistant");
                assistant.set("content", message.path("content").isTextual() ? message.get("content") : json.nullNode());
                assistant.set("tool_calls", calls);
                if (message.path("reasoning_content").isTextual()) {
                    assistant.set("reasoning_content", message.get("reasoning_content"));
                }
                for (JsonNode call : calls) {
                    JsonNode function = call.path("function");
                    String name = function.path("name").asText("");
                    JsonNode args = arguments(function.path("arguments").textValue());
                    Object result = toolNames.contains(name) && args.isObject()
                            ? executeTool.apply(name, args) : Map.of("error", "Unknown tool or invalid JSON arguments.");
                    messages.addObject().put("role", "tool").put("tool_call_id", call.path("id").asText())
                            .put("content", json.writeValueAsString(result));
                }
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
        } catch (JsonProcessingException | ExecutionException | TimeoutException failure) {
            Throwable cause = failure.getCause() == null ? failure : failure.getCause();
            LOG.debug("Chat provider request failed ({})", cause.getClass().getSimpleName());
        }
        return Optional.empty();
    }

    private JsonNode complete(ObjectNode body, long deadline)
            throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        String requestBody = json.writeValueAsString(body);
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) throw new TimeoutException();
        var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofNanos(remaining))
                .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
        var pending = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            var response = pending.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.debug("Chat provider returned HTTP {}", response.statusCode());
                return json.missingNode();
            }
            if (response.body().length() > MAX_RESPONSE_CHARS) return json.missingNode();
            return json.readTree(response.body()).path("choices").path(0);
        } finally {
            if (!pending.isDone()) pending.cancel(true);
        }
    }

    private JsonNode arguments(String raw) {
        if (raw == null || raw.length() > MAX_ARGUMENT_CHARS) return json.missingNode();
        try {
            return json.readTree(raw);
        } catch (JsonProcessingException failure) {
            return json.missingNode();
        }
    }

    record ChatMessage(String role, String content) {}

    record Tool(String name, String description, Map<String, Object> parameters) {}
}
