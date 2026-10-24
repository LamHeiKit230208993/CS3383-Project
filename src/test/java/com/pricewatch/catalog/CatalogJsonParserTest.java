package com.pricewatch.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.importer.CatalogJsonParser;
import com.pricewatch.catalog.importer.ImportedCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogJsonParserTest {
    private final CatalogJsonParser parser = new CatalogJsonParser(new ObjectMapper());

    @Test
    void readsTheUnchangedSampleAndKeepsLanguageLabelsAndOffers() throws IOException {
        byte[] sample;
        try (InputStream input = getClass().getResourceAsStream("/samples/pricewatch-products.json")) {
            sample = input.readAllBytes();
        }

        ImportedCatalog catalog = parser.parse(sample);

        assertEquals(2, catalog.products().size());
        assertEquals(8, catalog.listingCount());
        assertEquals(4, catalog.offerCount());
        ImportedCatalog.ImportedProduct milk = catalog.products().get(1);
        assertEquals("鮮牛奶 236毫升", milk.name().zhHant());
        assertEquals("鲜牛奶 236毫升", milk.name().zhHans());
        assertEquals("2 for $16.50", milk.listings().getFirst().offers().getFirst().en());
    }

    @Test
    void keepsBlankPriceAsUnknownAndKeepsOffersForRetailerWithoutPrice() {
        ImportedCatalog catalog = parser.parse(productJson("").getBytes(StandardCharsets.UTF_8));

        assertNull(catalog.products().getFirst().listings().getFirst().price());
        assertEquals("Offer", catalog.products().getFirst().listings().getFirst().offers().getFirst().en());
    }

    @Test
    void rejectsConflictingDuplicateProductsAndUnsupportedPriceScale() {
        String first = productJson("1.25").replace("\"prices\":[{\"supermarketCode\":\"STORE\",\"price\":\"1.25\"}],", "\"prices\":[],");
        String product = first.substring(first.indexOf('[') + 1, first.lastIndexOf(']')).trim();
        String conflict = "[" + product + ","
                + product.replace("\"name\":{\"en\":\"Pack 1L\"", "\"name\":{\"en\":\"Pack 2L\"") + "]";
        assertThrows(CatalogImportException.class,
                () -> parser.parse(conflict.getBytes(StandardCharsets.UTF_8)));

        assertThrows(CatalogImportException.class,
                () -> parser.parse(productJson("1.00001").getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsTrailingJsonDocuments() {
        String body = productJson("1.25").stripTrailing() + " []";

        assertThrows(CatalogImportException.class,
                () -> parser.parse(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsIntegralNumericProductCodesAsCanonicalText() {
        String body = productJson("1.25").replace("\"code\":\"PACK-1L\"", "\"code\":173520");

        assertEquals("173520", parser.parse(body.getBytes(StandardCharsets.UTF_8))
                .products().getFirst().code());
    }

    @Test
    void rejectsNegativeAndFractionalNumericProductCodes() {
        String body = productJson("1.25");

        assertThrows(CatalogImportException.class, () -> parser.parse(body
                .replace("\"code\":\"PACK-1L\"", "\"code\":-173520")
                .getBytes(StandardCharsets.UTF_8)));
        assertThrows(CatalogImportException.class, () -> parser.parse(body
                .replace("\"code\":\"PACK-1L\"", "\"code\":173520.5")
                .getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void detectsConflictsBetweenNumericAndTextCodesAfterNormalization() {
        String numeric = productJson("1.25").replace("\"code\":\"PACK-1L\"", "\"code\":173520");
        String text = productJson("1.25").replace("\"code\":\"PACK-1L\"", "\"code\":\"173520\"")
                .replace("\"name\":{\"en\":\"Pack 1L\"", "\"name\":{\"en\":\"Different pack\"");
        String combined = "[" + objectFromArray(numeric) + "," + objectFromArray(text) + "]";

        assertThrows(CatalogImportException.class,
                () -> parser.parse(combined.getBytes(StandardCharsets.UTF_8)));
    }

    private String objectFromArray(String json) {
        return json.substring(json.indexOf('[') + 1, json.lastIndexOf(']')).trim();
    }

    private String productJson(String price) {
        return """
                [{
                  "code":"PACK-1L",
                  "brand":{"en":"Brand","zh-Hant":"品牌","zh-Hans":"品牌"},
                  "name":{"en":"Pack 1L","zh-Hant":"一公升","zh-Hans":"一升"},
                  "cat1Name":{"en":"Food","zh-Hant":"食品","zh-Hans":"食品"},
                  "cat2Name":{"en":"Milk","zh-Hant":"牛奶","zh-Hans":"牛奶"},
                  "cat3Name":{"en":"Fresh","zh-Hant":"鮮奶","zh-Hans":"鲜奶"},
                  "prices":[{"supermarketCode":"STORE","price":"%s"}],
                  "offers":[{"supermarketCode":"STORE","en":"Offer","zh-Hant":"優惠","zh-Hans":"优惠"}]
                }]
                """.formatted(price);
    }
}
