package com.pricewatch.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpAiAnswerClientTest {
    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String content = "Checked answer";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String response = new ObjectMapper().writeValueAsString(Map.of(
                    "choices", new Object[]{Map.of("message", Map.of("content", content))}));
            exchange.sendResponseHeaders(status, response.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void sendsAnOpenAiCompatibleRequestAndReadsTheAnswer() {
        HttpAiAnswerClient client = client("test-key");
        String answer = client.answer(Map.of("productCode", "MILK", "lowestPrice", "10.00"), "How much is milk?");

        assertEquals("Checked answer", answer);
        assertEquals("/v1/chat/completions", path.get());
        assertEquals("Bearer test-key", authorization.get());
        assertTrue(body.get().contains("\"model\":\"CS3383/DeepSeek-V4-Flash\""));
        assertTrue(body.get().contains("\"stream\":false"));
        assertTrue(body.get().contains("How much is milk?"));
        assertTrue(body.get().contains("productCode"));
    }

    @Test
    void aProviderErrorUsesTheSafeFailure() {
        status = 500;
        HttpAiAnswerClient client = client("test-key");

        assertThrows(AiUnavailableException.class, () -> client.answer(Map.of(), "question"));
    }

    @Test
    void anEmptyAnswerUsesTheSafeFailure() {
        content = " ";
        HttpAiAnswerClient client = client("test-key");

        assertThrows(AiUnavailableException.class, () -> client.answer(Map.of(), "question"));
    }

    @Test
    void anEmptyKeyNeverCallsTheProvider() {
        HttpAiAnswerClient client = client("");

        assertThrows(AiUnavailableException.class, () -> client.answer(Map.of(), "question"));
    }

    private HttpAiAnswerClient client(String key) {
        return new HttpAiAnswerClient(new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                "CS3383/DeepSeek-V4-Flash", key, 5);
    }
}
