package com.pricewatch.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.catalog.SearchResults;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatIntentClientTest {
    @Test
    void providerLeadsClarificationSearchAndSamePackFollowUp() throws Exception {
        var responses = new ArrayDeque<>(List.of(
                "{\"action\":\"clarify\",\"field\":\"product\"}",
                "{\"action\":\"search\",\"query\":\"fresh milk\",\"retailer\":\"Wellcome\"}",
                "{\"action\":\"compare\",\"retailer\":",
                "```JSON\n{\"action\":\"compare\",\"retailer\":\"AEON\"}\n```",
                "{\"action\":\"converse\",\"field\":\"offTopic\"}",
                "Invented price: 0.01"));
        var requests = new java.util.concurrent.CopyOnWriteArrayList<String>();
        var json = new ObjectMapper();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try (exchange) {
                if (!"Bearer fixture-key".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                    exchange.sendResponseHeaders(401, -1);
                    return;
                }
                requests.add(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                byte[] response = json.writeValueAsBytes(Map.of("choices", List.of(
                        Map.of("message", Map.of("content", responses.removeFirst())))));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        server.start();
        var previousLocale = LocaleContextHolder.getLocaleContext();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            var provider = new ChatIntentClient("http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                    "fixture-key", "fixture-model");
            var catalog = mock(CatalogService.class);
            var milk = product("MILK-236", "Fresh Milk 236mL");
            when(catalog.searchResults(anyString(), eq(""))).thenReturn(
                    new SearchResults(List.of(), List.of(), List.of()));
            when(catalog.searchResults("fresh milk", "")).thenReturn(
                    new SearchResults(List.of(milk, product("MILK-1L", "Fresh Milk 1L")), List.of(), List.of()));
            when(catalog.find("MILK-236")).thenReturn(milk);
            var messages = new ResourceBundleMessageSource();
            messages.setBasename("chat-messages");
            messages.setDefaultEncoding("UTF-8");
            messages.setFallbackToSystemLocale(false);
            var ui = new AskController(catalog, provider, new HelloController(catalog, null, messages), messages);
            var session = new MockHttpSession();
            String redirect = ui.ask("How much is it?", "", session);
            String chatId = redirect.substring(redirect.indexOf('=') + 1);
            String clarification = messages(ui, session, chatId);
            assertTrue(clarification.contains("Which product should I check?"), clarification);
            verify(catalog, never()).searchResults(anyString(), anyString());

            ui.ask("Could you look up the milk from the fridge at Wellcome?", chatId, session);
            assertTrue(messages(ui, session, chatId).contains("Choose the exact product"));
            assertFalse(messages(ui, session, chatId).contains("12.00"));
            ui.ask("What about the smaller one?", chatId, session);
            assertEquals(2, AskController.history(session).getFirst().candidates().size(),
                    "An unusable provider turn must keep the active pack choices");
            ui.choose("MILK-236", chatId, session);
            assertTrue(messages(ui, session, chatId).contains("WELLCOME · HK$12.00"));

            ui.ask("And the other shop, AEON?", chatId, session);
            assertTrue(messages(ui, session, chatId).contains("Fresh Milk 236mL at AEON · HK$7.50"));
            assertTrue(requests.get(0).contains("fixture-model"));
            assertTrue(requests.get(1).contains("How much is it?"));
            assertTrue(requests.get(3).contains("Fresh Milk 236mL"));

            ui.ask("And at Wellcome?", chatId, session);
            assertEquals(4, requests.size(), "The exact retailer follow-up can use the local pack directly");
            assertEquals("Kowloon Dairy · Fresh Milk 236mL at WELLCOME · HK$12.00",
                    AskController.history(session).getFirst().messages().getLast().text());
            ui.ask("Can you tell me who is playing football tonight?", chatId, session);
            var retained = AskController.history(session).getFirst();
            assertEquals("MILK-236", retained.productCode());
            assertTrue(retained.messages().getLast().text().contains("help with shopping and prices"));
            assertTrue(provider.interpret("another question", "", List.of(), List.of()).isEmpty());
        } finally {
            LocaleContextHolder.setLocaleContext(previousLocale);
            server.stop(0);
        }
    }

    private static String messages(AskController ui, MockHttpSession session, String chatId) {
        var model = new ExtendedModelMap();
        ui.show(chatId, model, session);
        return model.get("messages").toString();
    }

    private static ProductView product(String code, String name) {
        var aeon = new ListingView("AEON", new BigDecimal("7.50"), List.of());
        var wellcome = new ListingView("WELLCOME", new BigDecimal("12.00"), List.of());
        return new ProductView(code, new LocalizedText("Kowloon Dairy", "", ""),
                new LocalizedText(name, "", ""), null, null, null, true,
                List.of(aeon, wellcome), aeon.price(), List.of(aeon));
    }
}
