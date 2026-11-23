package com.pricewatch.catalog;

import com.pricewatch.catalog.importer.ConsumerCouncilSourceClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumerCouncilSourceClientTest {
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/error", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.createContext("/large", exchange -> {
            byte[] body = new byte[ConsumerCouncilSourceClient.MAX_RESPONSE_BYTES + 1];
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/chunked-large", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var response = exchange.getResponseBody()) {
                response.write(new byte[ConsumerCouncilSourceClient.MAX_RESPONSE_BYTES + 1]);
            } finally {
                exchange.close();
            }
        });
        server.createContext("/disconnected", exchange -> exchange.close());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsSuccessfulResponseAndRejectsErrorsAndOversizedBodies() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        byte[] expected = "[]".getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(expected, new ConsumerCouncilSourceClient(URI.create(baseUrl + "/ok")).fetch());

        CatalogImportException failedStatus = assertThrows(CatalogImportException.class,
                () -> new ConsumerCouncilSourceClient(URI.create(baseUrl + "/error")).fetch());
        assertTrue(failedStatus.getMessage().contains("HTTP 503"));

        CatalogImportException tooLarge = assertThrows(CatalogImportException.class,
                () -> new ConsumerCouncilSourceClient(URI.create(baseUrl + "/large")).fetch());
        assertTrue(tooLarge.getMessage().contains("10 MB"));
    }

    @Test
    void enforcesTheLimitEvenWhenTheSourceDoesNotAdvertiseTheBodySize() {
        var source = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/chunked-large");
        var failure = assertThrows(CatalogImportException.class, () -> new ConsumerCouncilSourceClient(source).fetch());
        assertTrue(failure.getMessage().contains("10 MB"));
    }

    @Test
    void rejectsNonHttpSourcesAndWrapsAConnectionThatEndsWithoutAResponse() {
        var invalid = java.nio.file.Path.of("sample.json").toAbsolutePath().toUri();
        var schemeFailure = assertThrows(CatalogImportException.class, () -> new ConsumerCouncilSourceClient(invalid).fetch());
        assertTrue(schemeFailure.getMessage().contains("HTTP or HTTPS"));
        var disconnected = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/disconnected");
        var connectionFailure = assertThrows(CatalogImportException.class, () -> new ConsumerCouncilSourceClient(disconnected).fetch());
        assertTrue(connectionFailure.getMessage().contains("Could not retrieve prices"));
    }
}
