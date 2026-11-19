package com.pricewatch.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ImportStatus;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.catalog.SearchResults;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AskCatalogueToolsTest {
    private final ObjectMapper json = new ObjectMapper();
    private final CatalogService catalog = mock(CatalogService.class);

    @Test
    void sortsAllDirectMatchesByKnownPackPriceBeforeLimitingTheResults() throws Exception {
        var products = IntStream.rangeClosed(1, 10).mapToObj(index ->
                product("MILK-" + index, index == 1 ? null : BigDecimal.valueOf(20 - index))).toList();
        when(catalog.searchResults("milk", "")).thenReturn(new SearchResults(products,
                List.of(product("RELATED", BigDecimal.ONE)), List.of()));
        var tools = new AskCatalogueTools(catalog, Locale.ENGLISH, null, List.of());
        var search = execute(tools, "search_products", """
                {"query":"milk","sort":"lowest_listed_price"}
                """);
        assertEquals(10, search.path("total_matches").intValue());
        assertEquals(8, search.path("products").size());
        assertEquals("MILK-10", search.path("products").get(0).path("product_code").asText());
        assertEquals("MILK-3", search.path("products").get(7).path("product_code").asText());
        var cheapest = products.getLast();
        when(catalog.readProductSnapshot(cheapest.code())).thenReturn(new CatalogService.ProductSnapshot(cheapest, null));
        var checked = execute(tools, "get_product_prices", """
                {"product_code":"MILK-10"}
                """);
        assertEquals("MILK-10", checked.path("product_code").asText());
        assertEquals(10, checked.path("prices").get(0).path("price").intValue());
        assertEquals("MILK-10", tools.selectedCode());
    }

    @Test
    void limitsAmbiguousResultsToVerifiedPickerChoices() throws Exception {
        when(catalog.searchResults("milk", "")).thenReturn(new SearchResults(List.of(
                product("MILK-A", BigDecimal.TEN), product("MILK-B", BigDecimal.ONE)), List.of(), List.of()));
        var tools = new AskCatalogueTools(catalog, Locale.ENGLISH, null, List.of());
        assertTrue(execute(tools, "get_product_prices", """
                {"product_code":"MADE-UP"}
                """).has("error"));
        verifyNoInteractions(catalog);
        execute(tools, "search_products", """
                {"query":"milk"}
                """);
        assertTrue(execute(tools, "get_product_prices", """
                {"product_code":"MILK-A"}
                """).has("error"));
        assertTrue(execute(tools, "offer_product_choices", """
                {"product_codes":["MILK-A","MADE-UP"]}
                """).has("error"));
        assertTrue(tools.choices().isEmpty());
        var offered = execute(tools, "offer_product_choices", """
                {"product_codes":["MILK-B","MILK-A"]}
                """);
        assertEquals("MILK-B", offered.path("product_codes").get(0).asText());
        assertEquals(List.of("MILK-B", "MILK-A"), tools.choices().stream().map(AskController.Choice::code).toList());
        assertTrue(tools.priceCards().isEmpty());
    }

    @Test
    void selectedPackReturnsUnknownForMissingRetailerAndKeepsItsPriceRetrievalTime() throws Exception {
        var pack = product("MILK-A", BigDecimal.TEN);
        var retrievedAt = Instant.parse("2026-10-24T10:00:00Z");
        var accepted = new ImportStatus("SAMPLE", "https://example.test/catalogue", retrievedAt, null, "hash", 2, 2, 0);
        when(catalog.readProductSnapshot(pack.code())).thenReturn(new CatalogService.ProductSnapshot(pack, accepted));
        var tools = new AskCatalogueTools(catalog, Locale.ENGLISH, pack.code(), List.of());
        var reply = execute(tools, "get_product_prices", """
                {"retailer":"Missing store"}
                """);
        var row = reply.path("prices").get(0);
        assertEquals("Missing store", row.path("retailer").asText());
        assertTrue(row.path("price").isNull());
        assertEquals(retrievedAt.toString(), reply.path("source").path("retrieved_at").asText());
        assertEquals(retrievedAt, tools.priceCards().getFirst().retrievedAt());
    }

    @Test
    void resolvesBilingualAndChineseRetailerNamesWithoutGuessingUnknownRetailers() {
        var base = product("MILK-A", BigDecimal.TEN);
        var wellcome = new ListingView("WELLCOME", BigDecimal.TEN, List.of());
        var mannings = new ListingView("MANNINGS", BigDecimal.valueOf(12), List.of());
        var pack = new ProductView(base.code(), base.brand(), base.name(), base.category1(), base.category2(),
                base.category3(), true, List.of(wellcome, mannings), BigDecimal.TEN, List.of(wellcome));
        when(catalog.readProductSnapshot(pack.code())).thenReturn(new CatalogService.ProductSnapshot(pack, null));
        var tools = new AskCatalogueTools(catalog, Locale.ENGLISH, pack.code(), List.of());

        for (String variant : List.of("Wellcome", "惠康", "惠康 WELLCOME", "WELLCOME（惠康）")) {
            var row = priceAt(tools, variant);
            assertEquals("WELLCOME", row.path("retailer").asText());
            assertEquals(10, row.path("price").intValue());
        }
        assertEquals("MANNINGS", priceAt(tools, "万宁").path("retailer").asText());
        assertTrue(priceAt(tools, "Other Wellcome").path("price").isNull());
    }

    private JsonNode execute(AskCatalogueTools tools, String name, String args) throws Exception {
        return json.valueToTree(tools.execute(name, json.readTree(args)));
    }

    private JsonNode priceAt(AskCatalogueTools tools, String retailer) {
        return json.<JsonNode>valueToTree(tools.execute("get_product_prices", json.createObjectNode().put("retailer", retailer)))
                .path("prices").get(0);
    }

    private static ProductView product(String code, BigDecimal price) {
        var empty = new LocalizedText("", "", "");
        var name = new LocalizedText("Fresh Milk 236mL", "鮮牛奶 236毫升", "鲜牛奶 236毫升");
        var listing = new ListingView("AEON", price, List.of());
        return new ProductView(code, empty, name, empty, empty, empty, true, List.of(listing), price,
                price == null ? List.of() : List.of(listing));
    }
}
