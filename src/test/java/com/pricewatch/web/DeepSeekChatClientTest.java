package com.pricewatch.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DeepSeekChatClientTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<DeepSeekChatClient.ChatMessage> HISTORY = List.of(
            new DeepSeekChatClient.ChatMessage("user", "Hi"),
            new DeepSeekChatClient.ChatMessage("assistant", "Hello!"),
            new DeepSeekChatClient.ChatMessage("user", "Can you speak Traditional Chinese?"));
    private static final List<DeepSeekChatClient.Tool> TOOLS = List.of(
            new DeepSeekChatClient.Tool("search_products", "Search matching product packs.",
                    Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string")))),
            new DeepSeekChatClient.Tool("get_product_prices", "Read listed prices for a pack.",
                    Map.of("type", "object", "properties", Map.of("product_code", Map.of("type", "string")))));

    @Test
    void returnsNaturalTextAndSendsConversationWithRoles() throws Exception {
        try (var provider = new Fixture(answer(" 可以，我可以用繁體中文幫你比較價格。 "))) {
            assertEquals(Optional.of("可以，我可以用繁體中文幫你比較價格。"), provider.client().reply(
                    "Help shoppers compare prices.", HISTORY, TOOLS, (name, args) -> fail("No tool was requested")));

            JsonNode request = provider.requests.getFirst();
            assertEquals("fixture-model", request.path("model").asText());
            assertEquals("disabled", request.path("thinking").path("type").asText());
            assertEquals("auto", request.path("tool_choice").asText());
            assertEquals("search_products", request.path("tools").path(0).path("function").path("name").asText());
            JsonNode messages = request.path("messages");
            assertEquals(4, messages.size());
            assertEquals("system", messages.path(0).path("role").asText());
            assertEquals("Help shoppers compare prices.", messages.path(0).path("content").asText());
            assertEquals("assistant", messages.path(2).path("role").asText());
            assertEquals("Hello!", messages.path(2).path("content").asText());
            assertEquals("Can you speak Traditional Chinese?", messages.path(3).path("content").asText());
        }
    }

    @Test
    void correlatesToolResultsAcrossRoundsAndKeepsProviderReasoningOutOfReply() throws Exception {
        try (var provider = new Fixture(
                calls(call("call-a", "search_products", "{\"query\":\"milk\"}"),
                        call("call-b", "search_products", "{\"query\":\"fresh milk\"}")),
                calls(call("call-c", "get_product_prices", "{\"product_code\":\"MILK-236\"}")),
                answer("I checked the exact 236 mL pack."))) {
            var executed = new ArrayList<String>();
            assertEquals(Optional.of("I checked the exact 236 mL pack."), provider.client().reply(
                    "Compare prices with the tools.", HISTORY, TOOLS, (name, arguments) -> {
                        executed.add(name);
                        return name.equals("search_products")
                                ? Map.of("query", arguments.path("query").asText(), "productCode", "MILK-236")
                                : Map.of("amount", "7.50", "productCode", arguments.path("product_code").asText());
                    }));
            assertEquals(List.of("search_products", "search_products", "get_product_prices"), executed);

            JsonNode second = provider.requests.get(1).path("messages");
            assertEquals("assistant", second.path(4).path("role").asText());
            assertTrue(second.path(4).path("content").isNull());
            assertEquals("internal fixture reasoning", second.path(4).path("reasoning_content").asText());
            assertResult(second.path(5), "call-a", "query", "milk");
            assertResult(second.path(6), "call-b", "query", "fresh milk");
            JsonNode third = provider.requests.getLast().path("messages");
            assertResult(third.path(8), "call-c", "amount", "7.50");
        }
    }

    @Test
    void reportsUnsupportedToolsAndInvalidArgumentsToTheProviderForRecovery() throws Exception {
        try (var provider = new Fixture(
                calls(call("unknown", "save_list", "{}"), call("invalid", "search_products", "{} {}"),
                        call("empty", "search_products", ""), call("array", "search_products", "[]")),
                answer("What kind of milk should I look up?"))) {
            assertEquals(Optional.of("What kind of milk should I look up?"), provider.client().reply(
                    "Ask for clarification.", HISTORY, TOOLS, (name, args) -> fail("Invalid call reached catalogue")));
            JsonNode recovery = provider.requests.getLast().path("messages");
            String validationError = "Unknown tool or invalid JSON arguments.";
            assertResult(recovery.path(5), "unknown", "error", validationError);
            assertResult(recovery.path(6), "invalid", "error", validationError);
            assertResult(recovery.path(7), "empty", "error", validationError);
            assertResult(recovery.path(8), "array", "error", validationError);
        }
    }

    @Test
    void rejectsDuplicateCallIdsAndEndsAfterThreeToolRounds() throws Exception {
        Map<String, Object> toolCall = call("first", "search_products", "{\"query\":\"milk\"}");
        try (var provider = new Fixture(calls(toolCall, toolCall))) {
            assertTrue(provider.client().reply("Find a pack.", HISTORY, TOOLS,
                    (name, args) -> fail("Invalid batch executed a tool")).isEmpty());
        }
        try (var provider = new Fixture(calls(toolCall),
                calls(call("second", "search_products", "{}")), calls(call("third", "search_products", "{}")),
                calls(call("fourth", "search_products", "{}")))) {
            var executed = new ArrayList<String>();
            assertTrue(provider.client().reply("Find a pack.", HISTORY, TOOLS, (name, args) -> {
                executed.add(name);
                return Map.of();
            }).isEmpty());
            assertEquals(List.of("search_products", "search_products", "search_products"), executed);
            assertEquals(4, provider.requests.size());
        }
    }

    @Test
    void rejectsUnusableProviderResponsesAndMakesNoRequestWithoutCredentials() throws Exception {
        try (var provider = new Fixture(new Response(503, "{}"), new Response(200, "null"),
                new Response(200, "not JSON"), new Response(200, ""),
                response("length", Map.of("role", "assistant", "content", "Partial text")), answer(" "))) {
            for (int i = 0; i < 6; i++) {
                assertTrue(provider.client().reply("Compare prices.", HISTORY, TOOLS,
                        (name, args) -> fail("Unusable response executed a tool")).isEmpty());
            }
            var noCredentials = new DeepSeekChatClient(provider.url, "", "fixture-model");
            assertTrue(noCredentials.reply("Compare prices.", HISTORY, TOOLS, (name, args) -> fail()).isEmpty());
            assertEquals(6, provider.requests.size());
        }
    }

    private static void assertResult(JsonNode message, String id, String field, String value) throws IOException {
        assertEquals("tool", message.path("role").asText());
        assertEquals(id, message.path("tool_call_id").asText());
        assertEquals(value, JSON.readTree(message.path("content").asText()).path(field).asText());
    }

    private static Response answer(String text) throws IOException {
        return response("stop", Map.of("role", "assistant", "content", text));
    }

    @SafeVarargs
    private static Response calls(Map<String, Object>... calls) throws IOException {
        var message = JSON.createObjectNode().put("role", "assistant").putNull("content")
                .put("reasoning_content", "internal fixture reasoning");
        message.set("tool_calls", JSON.valueToTree(List.of(calls)));
        return response("tool_calls", message);
    }

    private static Map<String, Object> call(String id, String name, String arguments) {
        return Map.of("id", id, "type", "function", "function", Map.of("name", name, "arguments", arguments));
    }

    private static Response response(String reason, Object message) throws IOException {
        return new Response(200, JSON.writeValueAsString(Map.of("choices", List.of(
                Map.of("finish_reason", reason, "message", message)))));
    }

    private record Response(int status, String body) {}

    private static class Fixture implements AutoCloseable {
        private final HttpServer server;
        private final String url;
        private final List<JsonNode> requests = new CopyOnWriteArrayList<>();

        Fixture(Response... responses) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            server.createContext("/v1/chat/completions", exchange -> {
                try (exchange) {
                    if (!"Bearer fixture-key".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                        exchange.sendResponseHeaders(401, -1);
                        return;
                    }
                    int index = requests.size();
                    requests.add(JSON.readTree(exchange.getRequestBody()));
                    if (index >= responses.length) {
                        exchange.sendResponseHeaders(500, -1);
                        return;
                    }
                    Response response = responses[index];
                    byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(response.status(), bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
            });
            server.start();
        }

        DeepSeekChatClient client() {
            return new DeepSeekChatClient(url, "fixture-key", "fixture-model");
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
