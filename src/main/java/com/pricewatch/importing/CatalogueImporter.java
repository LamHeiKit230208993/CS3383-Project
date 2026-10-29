package com.pricewatch.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.domain.Catalogue;
import com.pricewatch.domain.Listing;
import com.pricewatch.domain.LocalisedName;
import com.pricewatch.domain.Product;
import com.pricewatch.store.CatalogueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CatalogueImporter {

    private static final String SAMPLE = "samples/pricewatch-products.json";
    private static final List<String> LANGUAGES = List.of("en", "zh-Hant", "zh-Hans");

    private final CatalogueStore store;
    private final ObjectMapper mapper;
    private final String dailyUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public CatalogueImporter(CatalogueStore store, ObjectMapper mapper,
                             @Value("${pricewatch.daily-url:}") String dailyUrl) {
        this.store = store;
        this.mapper = mapper;
        this.dailyUrl = dailyUrl;
    }

    public ImportResult importSample() {
        try (InputStream in = new ClassPathResource(SAMPLE).getInputStream()) {
            return importJson(in.readAllBytes(), "sample:" + SAMPLE);
        } catch (IOException e) {
            return ImportResult.rejected("Sample could not be read: " + e.getMessage());
        }
    }

    public ImportResult importDaily() {
        if (dailyUrl == null || dailyUrl.isBlank()) {
            return importSample();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(dailyUrl))
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                return ImportResult.rejected("Daily download returned HTTP " + response.statusCode());
            }
            return importJson(response.body(), dailyUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ImportResult.rejected("Daily download was interrupted");
        } catch (Exception e) {
            return ImportResult.rejected("Daily download failed: " + e.getMessage());
        }
    }

    public ImportResult importJson(byte[] json, String sourceUrl) {
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (IOException e) {
            return ImportResult.rejected("Response is not valid JSON");
        }
        if (root == null || !root.isArray()) {
            return ImportResult.rejected("Response is not a product array");
        }
        if (root.isEmpty()) {
            return ImportResult.rejected("Response contains no products");
        }

        List<Product> products = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        for (JsonNode node : root) {
            String code = node.path("code").asText("");
            if (code.isBlank()) {
                return ImportResult.rejected("A product has no code");
            }
            if (!codes.add(code)) {
                return ImportResult.rejected("Duplicate product code " + code);
            }
            LocalisedName brand = label(node.get("brand"));
            LocalisedName name = label(node.get("name"));
            if (brand == null || name == null) {
                return ImportResult.rejected("Product " + code + " is missing a language label");
            }
            LocalisedName cat1 = label(node.get("cat1Name"));
            LocalisedName cat2 = label(node.get("cat2Name"));
            LocalisedName cat3 = label(node.get("cat3Name"));
            if (cat1 == null || cat2 == null || cat3 == null) {
                return ImportResult.rejected("Product " + code + " is missing a category label");
            }

            JsonNode prices = node.get("prices");
            if (prices != null && !prices.isNull() && !prices.isArray()) {
                return ImportResult.rejected("Product " + code + " prices is not an array");
            }
            List<Listing> listings = new ArrayList<>();
            Set<String> retailers = new HashSet<>();
            for (JsonNode price : node.path("prices")) {
                String retailer = price.path("supermarketCode").asText("");
                if (retailer.isBlank() || !retailers.add(retailer)) {
                    return ImportResult.rejected("Product " + code + " has a bad or repeated retailer listing");
                }
                listings.add(new Listing(retailer, money(price.path("price").asText(null)), offer(node, retailer)));
            }
            if (listings.isEmpty() && !node.path("offers").isEmpty()) {
                return ImportResult.rejected("Product " + code + " has offers but no price rows");
            }
            products.add(new Product(code, brand, name, cat1, cat2, cat3, listings));
        }

        Catalogue catalogue = new Catalogue(Instant.now(), null, sourceUrl, products);
        return ImportResult.accepted(store.replace(catalogue));
    }

    private LocalisedName label(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String en = node.path("en").asText("");
        String hant = node.path("zh-Hant").asText("");
        String hans = node.path("zh-Hans").asText("");
        if (en.isBlank() || hant.isBlank() || hans.isBlank()) {
            return null;
        }
        return new LocalisedName(en, hant, hans);
    }

    private LocalisedName offer(JsonNode product, String retailer) {
        for (JsonNode offer : product.path("offers")) {
            if (retailer.equals(offer.path("supermarketCode").asText(""))) {
                return label(offer);
            }
        }
        return new LocalisedName("", "", "");
    }

    private BigDecimal money(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
