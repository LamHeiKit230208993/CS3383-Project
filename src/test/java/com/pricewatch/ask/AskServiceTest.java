package com.pricewatch.ask;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ImportStatus;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.catalog.SearchResults;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AskServiceTest {
    private static final Instant RETRIEVED = Instant.parse("2026-11-18T04:00:00Z");
    private final CatalogService catalog = mock(CatalogService.class);
    private final RecordingClient ai = new RecordingClient();

    @Test
    void oneProductSendsOnlyItsCheckedFacts() {
        stubProducts(product("MILK", "Milk", "Dairy",
                listing("AEON", "10.00"), listing("JASONS", "12.50"), listing("CITY", null)));
        ai.answer = "Milk costs HK$10.00 at AEON. CITY has no listed price.";

        AskResult result = new AskService(catalog, ai).ask("How much is milk?", "");

        assertEquals(AskResult.Status.ANSWERED, result.status());
        assertFalse(result.fallback());
        assertEquals(ai.answer, result.answer());
        assertEquals(1, ai.calls.get());
        assertEquals("MILK", ai.facts.get("productCode"));
        assertEquals("unknown", ai.facts.get("sourcePriceDate"));
        assertEquals(RETRIEVED.toString(), ai.facts.get("retrievedAt"));
        assertEquals("10.00", ai.facts.get("lowestPrice"));
        assertEquals(3, result.facts().prices().size());
        assertEquals(List.of("AEON"), result.facts().lowestRetailers());
        assertNull(result.facts().prices().get(2).price());
    }

    @Test
    void severalProductsProduceAChooserWithoutCallingTheModel() {
        stubProducts(product("MILK", "Milk", "Dairy", listing("AEON", "10.00")),
                product("BREAD", "Bread", "Bakery", listing("AEON", "8.00")));

        AskResult result = new AskService(catalog, ai).ask("Compare milk and bread", "");

        assertEquals(AskResult.Status.CHOOSE, result.status());
        assertEquals(2, result.candidates().size());
        assertEquals(0, ai.calls.get());
        assertNull(result.facts());
    }

    @Test
    void noProductProducesAnExplicitNoMatch() {
        stubProducts(product("MILK", "Milk", "Dairy", listing("AEON", "10.00")));

        AskResult result = new AskService(catalog, ai).ask("How much is a laptop?", "");

        assertEquals(AskResult.Status.NO_MATCH, result.status());
        assertEquals(0, ai.calls.get());
        assertNull(result.facts());
    }

    @Test
    void chosenProductResolvesAmbiguity() {
        stubProducts(product("MILK", "Milk", "Dairy", listing("AEON", "10.00")),
                product("BREAD", "Bread", "Bakery", listing("AEON", "8.00")));
        ai.answer = "Bread costs HK$8.00 at AEON.";

        AskResult result = new AskService(catalog, ai).ask("Compare milk and bread", "BREAD");

        assertEquals(AskResult.Status.ANSWERED, result.status());
        assertEquals("BREAD", result.facts().productCode());
        assertEquals("Bread costs HK$8.00 at AEON.", result.answer());
    }

    @Test
    void anAmountOutsideTheCheckedFactsIsRejected() {
        stubProducts(product("MILK", "Milk", "Dairy", listing("AEON", "10.00")));
        ai.answer = "Milk costs HK$99.00 at AEON.";

        AskResult result = new AskService(catalog, ai).ask("How much is milk?", "");

        assertEquals(AskResult.Status.ANSWERED, result.status());
        assertTrue(result.fallback());
        assertTrue(result.answer().contains("HK$10.00"));
        assertFalse(result.answer().contains("99.00"));
    }

    @Test
    void aModelFailureLeavesTheCheckedAnswer() {
        stubProducts(product("MILK", "Milk", "Dairy", listing("AEON", "10.00")));
        ai.failure = new AiUnavailableException("offline");

        AskResult result = new AskService(catalog, ai).ask("How much is milk?", "");

        assertEquals(AskResult.Status.ANSWERED, result.status());
        assertTrue(result.fallback());
        assertEquals(1, result.facts().prices().size());
        assertTrue(result.answer().contains("HK$10.00"));
    }

    @Test
    void anUnpricedProductUsesTheUnknownFallback() {
        stubProducts(product("MILK", "Milk", "Dairy", listing("AEON", null)));
        ai.failure = new AiUnavailableException("offline");

        AskResult result = new AskService(catalog, ai).ask("How much is milk?", "");

        assertTrue(result.fallback());
        assertTrue(result.answer().contains("price is unknown"));
        assertNull(result.facts().lowestPrice());
    }

    @Test
    void blankAndLongQuestionsAreRejected() {
        AskService service = new AskService(catalog, ai);
        assertThrows(IllegalArgumentException.class, () -> service.ask("  ", ""));
        assertThrows(IllegalArgumentException.class, () -> service.ask("x".repeat(501), ""));
        assertEquals(0, ai.calls.get());
    }

    @Test
    void unknownSelectedProductIsRejected() {
        stubProducts(product("MILK", "Milk", "Dairy", listing("AEON", "10.00")));

        assertThrows(IllegalArgumentException.class,
                () -> new AskService(catalog, ai).ask("How much is milk?", "MISSING"));
        assertEquals(0, ai.calls.get());
    }

    private void stubProducts(ProductView... products) {
        ImportStatus status = new ImportStatus("LIVE", "https://example.test", RETRIEVED,
                LocalDate.of(2026, 11, 17), "checksum", products.length, 1, 0);
        when(catalog.searchResults("", "")).thenReturn(new SearchResults(List.of(products), List.of(), List.of()));
        when(catalog.status()).thenReturn(status);
    }

    private static ProductView product(String code, String name, String category, ListingView... listings) {
        List<ListingView> list = List.of(listings);
        BigDecimal lowest = list.stream().map(ListingView::price).filter(price -> price != null)
                .min(BigDecimal::compareTo).orElse(null);
        List<ListingView> lowestListings = lowest == null ? List.of() : list.stream()
                .filter(listing -> listing.price() != null && lowest.compareTo(listing.price()) == 0).toList();
        return new ProductView(code, new LocalizedText("Test brand", null, null),
                new LocalizedText(name, null, null), new LocalizedText("Food", null, null),
                new LocalizedText(category, null, null), new LocalizedText(category, null, null),
                true, list, lowest, lowestListings);
    }

    private static ListingView listing(String retailer, String price) {
        return new ListingView(retailer, price == null ? null : new BigDecimal(price), List.of());
    }

    private static final class RecordingClient implements AiAnswerClient {
        private final AtomicInteger calls = new AtomicInteger();
        private Map<String, Object> facts;
        private String answer = "Checked answer";
        private RuntimeException failure;

        @Override
        public String answer(Map<String, Object> checkedFacts, String question) {
            calls.incrementAndGet();
            facts = checkedFacts;
            if (failure != null) throw failure;
            return answer;
        }
    }
}
