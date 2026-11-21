package com.pricewatch.chat;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.catalog.SearchResults;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Checks the tool-calling loop and fallback without a real provider. */
class ChatAgentTest {
    private final CatalogService catalog = mock(CatalogService.class);

    private static final ProductView MILK = product("MILK", "Fresh Milk 236mL", "7.50", "AEON");
    private static final ProductView BREAD = product("BREAD", "Crustless Bread", "15.00", "PARKNSHOP");

    @Test
    void loopExecutesSearchThenPricesAndReturnsModelAnswer() {
        AiClient ai = scripted(
                new ChatCompletion("", List.of(new ToolCall("1", "search_products", "{\"query\":\"milk\"}"))),
                new ChatCompletion("", List.of(new ToolCall("2", "product_prices", "{\"code\":\"MILK\"}"))),
                new ChatCompletion("Milk is HK$7.50 at AEON.", List.of()));
        when(catalog.catalogue()).thenReturn(List.of(MILK, BREAD));
        when(catalog.searchResults(anyString(), anyString()))
                .thenReturn(new SearchResults(List.of(MILK), List.of(), List.of()));
        when(catalog.find("MILK")).thenReturn(MILK);

        var reply = new ChatService(catalog, ai).reply("cheapest milk");

        assertEquals("answer", reply.kind());
        assertTrue(reply.message().contains("HK$7.50"));
        assertTrue(reply.checkedFacts().contains("Retailer AEON: 7.50 HKD"));
    }

    @Test
    void unknownProductCodeToolReturnsNotFound() {
        AiClient ai = scripted(
                new ChatCompletion("", List.of(new ToolCall("1", "product_prices", "{\"code\":\"NOPE\"}"))),
                new ChatCompletion("I could not find that product.", List.of()));
        when(catalog.catalogue()).thenReturn(List.of(MILK));
        when(catalog.find("NOPE")).thenThrow(new IllegalArgumentException("missing"));

        var reply = new ChatService(catalog, ai).reply("something");

        assertEquals("answer", reply.kind());
        assertTrue(reply.checkedFacts().isEmpty());
    }

    @Test
    void stepLimitForcesAFinalAnswerWithoutTools() {
        AiClient ai = new AiClient() {
            int calls;
            public boolean isConfigured() { return true; }
            public ChatCompletion completeTurn(List<ChatMessage> messages, List<ToolSpec> tools) {
                calls++;
                if (tools == null) return new ChatCompletion("Final summary.", List.of());
                return new ChatCompletion("", List.of(new ToolCall("c" + calls, "search_products", "{\"query\":\"x\"}")));
            }
        };
        when(catalog.catalogue()).thenReturn(List.of(MILK));
        when(catalog.searchResults(anyString(), anyString())).thenReturn(new SearchResults(List.of(), List.of(), List.of()));

        var reply = new ChatService(catalog, ai).reply("loop forever");
        assertEquals("answer", reply.kind());
        assertEquals("Final summary.", reply.message());
    }

    private static AiClient scripted(ChatCompletion... turns) {
        return new AiClient() {
            int index;
            public boolean isConfigured() { return true; }
            public ChatCompletion completeTurn(List<ChatMessage> messages, List<ToolSpec> tools) {
                return turns[Math.min(index++, turns.length - 1)];
            }
        };
    }

    private static ProductView product(String code, String name, String price, String retailer) {
        BigDecimal amount = new BigDecimal(price);
        var listing = new ListingView(retailer, amount, List.of());
        return new ProductView(code, new LocalizedText("Brand", "", ""), new LocalizedText(name, "", ""),
                new LocalizedText("Food", "", ""), new LocalizedText("Milk", "", ""),
                new LocalizedText("Fresh", "", ""), true, List.of(listing), amount, List.of(listing));
    }
}
