package com.pricewatch.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ImportStatus;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.ProductView;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Catalogue evidence and product choices collected for a single reply. */
public final class AskCatalogueTools {
    private static final int MAX_RESULTS = 8;
    private static final Map<String, List<String>> RETAILER_ALIASES = Map.of(
            "wellcome", List.of("惠康", "惠康超級市場", "惠康超级市场", "惠康超市"),
            "parknshop", List.of("百佳", "百佳超級市場", "百佳超级市场", "百佳超市"),
            "aeon", List.of("永旺"),
            "mannings", List.of("萬寧", "万宁"),
            "watsons", List.of("屈臣氏"),
            "sasa", List.of("莎莎"),
            "dchfood", List.of("大昌食品"),
            "lungfung", List.of("龍豐", "龙丰"));
    static final List<DeepSeekChatClient.Tool> TOOLS = List.of(
            new DeepSeekChatClient.Tool("search_products", "Find exact pack candidates in the saved catalogue.",
                    schema(Map.of("query", Map.of("type", "string", "maxLength", 120,
                                    "description", "Short product terms or exact product code. Keep the brand and pack constraints."),
                            "sort", Map.of("type", "string", "enum", List.of("relevance", "lowest_listed_price"))),
                            List.of("query"))),
            new DeepSeekChatClient.Tool("get_product_prices", "Read saved listed prices for one exact pack.",
                    schema(Map.of("product_code", Map.of("type", "string", "maxLength", 80,
                                    "description", "Exact code from search; omit for the already selected pack."),
                            "retailer", Map.of("type", "string", "maxLength", 80,
                                    "description", "Retailer name or code. Omit to compare all known listings.")), List.of())),
            new DeepSeekChatClient.Tool("offer_product_choices", "Show the shopper a picker when multiple packs could match.",
                    schema(Map.of("product_codes", Map.of("type", "array", "items", Map.of("type", "string"),
                            "minItems", 2, "maxItems", MAX_RESULTS)), List.of("product_codes"))));

    private final CatalogService catalogue;
    private final Locale locale;
    private final List<PriceCard> priceCards = new ArrayList<>();
    private final Map<String, AskController.Choice> discovered = new LinkedHashMap<>();
    private List<AskController.Choice> choices;
    private String exactSearchCode;
    private String selectedCode;

    AskCatalogueTools(CatalogService catalogue, Locale locale, String selectedCode, List<AskController.Choice> choices) {
        this.catalogue = catalogue;
        this.locale = locale;
        this.selectedCode = selectedCode;
        this.choices = choices;
        choices.forEach(choice -> discovered.put(choice.code(), choice));
    }

    Object execute(String name, JsonNode arguments) {
        if (!arguments.isObject()) return error("Tool arguments must be an object.");
        try {
            return switch (name) {
                case "search_products" -> search(argument(arguments, "query", 120), argument(arguments, "sort", 22));
                case "get_product_prices" -> prices(argument(arguments, "product_code", 80), argument(arguments, "retailer", 80));
                case "offer_product_choices" -> offer(arguments.path("product_codes"));
                default -> error("Unknown catalogue tool.");
            };
        } catch (InvalidArguments failure) {
            return error(failure.getMessage());
        }
    }

    private Object search(String query, String sort) {
        if (query.isBlank()) return error("Provide non-blank product search terms.");
        if (!sort.isBlank() && !sort.equals("relevance") && !sort.equals("lowest_listed_price")) {
            return error("Sort must be relevance or lowest_listed_price.");
        }
        boolean cheapest = sort.equals("lowest_listed_price");
        List<ProductView> matches = catalogue.searchResults(query, "").matches();
        var exact = matches.stream().filter(product -> product.code().equals(query)).findFirst();
        if (exact.isPresent()) matches = List.of(exact.get());
        if (cheapest) {
            matches = matches.stream().sorted(Comparator.comparing(ProductView::lowestPrice,
                    Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        }

        exactSearchCode = null;
        if (matches.size() == 1 || cheapest && !matches.isEmpty() && matches.getFirst().lowestPrice() != null) {
            exactSearchCode = matches.getFirst().code();
        }
        discovered.clear();
        var products = matches.stream().limit(MAX_RESULTS).map(product -> {
            discovered.put(product.code(), choice(product, locale));
            return new SearchProduct(product.code(), label(product, locale), product.category3().forLocale(locale),
                    product.lowestPrice());
        }).toList();
        return new SearchReply(products, matches.size(), cheapest ? sort : "relevance", source(catalogue.status()));
    }

    private Object prices(String code, String retailer) {
        String requestedCode = code.isBlank() ? selectedCode : code;
        if (requestedCode == null || !(requestedCode.equals(selectedCode) || requestedCode.equals(exactSearchCode))) {
            return error("Search for an exact pack or ask the shopper to choose from matching packs first.");
        }
        CatalogService.ProductSnapshot snapshot;
        try {
            snapshot = catalogue.readProductSnapshot(requestedCode);
        } catch (IllegalArgumentException ignored) {
            return error("This exact pack is unavailable in the accepted catalogue. Search again.");
        }
        var product = snapshot.product();
        if (!product.active()) {
            return error("This exact pack is unavailable in the accepted catalogue. Search again.");
        }

        String requestedRetailer = retailerKey(retailer);
        List<PriceRow> rows = product.listings().stream()
                .filter(listing -> retailer.isBlank() || matchesRetailer(listing.retailerCode(), requestedRetailer))
                .map(listing -> row(product, listing)).toList();
        if (rows.isEmpty() && !retailer.isBlank()) rows = List.of(new PriceRow(retailer, null, false));
        var status = snapshot.status();
        var card = new PriceCard(product.code(), label(product, locale), rows, status == null ? null : status.retrievedAt());
        if (!priceCards.contains(card)) priceCards.add(card);
        selectedCode = product.code();
        choices = List.of();
        return new PriceReply(card.productCode(), card.productLabel(), card.rows(), source(status));
    }

    private Object offer(JsonNode codes) {
        if (!codes.isArray() || codes.size() < 2 || codes.size() > MAX_RESULTS) {
            return error("Choose two to eight codes from the search results.");
        }
        var offered = new LinkedHashMap<String, AskController.Choice>();
        for (JsonNode code : codes) {
            var choice = code.isTextual() ? discovered.get(code.textValue()) : null;
            if (choice == null || offered.put(choice.code(), choice) != null) {
                return error("Use distinct exact product codes from the search results.");
            }
        }
        choices = List.copyOf(offered.values());
        return Map.of("product_codes", List.copyOf(offered.keySet()));
    }

    String selectedCode() {
        return selectedCode;
    }

    String exactSearchCode() {
        return exactSearchCode;
    }

    List<AskController.Choice> choices() {
        return choices;
    }

    List<PriceCard> priceCards() {
        return List.copyOf(priceCards);
    }

    static ProductView activeProduct(CatalogService catalogue, String code) {
        try {
            var product = catalogue.find(code);
            return product.active() ? product : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static AskController.Choice choice(ProductView product, Locale locale) {
        return new AskController.Choice(product.code(), label(product, locale) + " · " + product.code());
    }

    private static String label(ProductView product, Locale locale) {
        String brand = product.brand().forLocale(locale);
        return (brand.isBlank() ? "" : brand + " · ") + product.name().forLocale(locale);
    }

    private static Source source(ImportStatus status) {
        return status == null ? null : new Source(status.origin(), status.sourceUrl(),
                status.retrievedAt() == null ? null : status.retrievedAt().toString(),
                status.sourceDate() == null ? null : status.sourceDate().toString());
    }

    private static PriceRow row(ProductView product, ListingView listing) {
        return new PriceRow(listing.retailerCode(), listing.price(), product.lowestListings().contains(listing));
    }

    private static String retailerKey(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static boolean matchesRetailer(String retailerCode, String requestedKey) {
        String code = retailerKey(retailerCode);
        return code.equals(requestedKey) || RETAILER_ALIASES.getOrDefault(code, List.of()).stream()
                .anyMatch(alias -> requestedKey.equals(alias) || requestedKey.equals(alias + code)
                        || requestedKey.equals(code + alias));
    }

    private static String argument(JsonNode arguments, String key, int maxLength) {
        var value = arguments.get(key);
        if (value == null) return "";
        if (!value.isTextual() || value.textValue().length() > maxLength) {
            throw new InvalidArguments("Use a short string for " + key + ".");
        }
        return value.textValue().strip();
    }

    private static Map<String, String> error(String message) {
        return Map.of("error", message);
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        return Map.of("type", "object", "properties", properties, "required", required, "additionalProperties", false);
    }

    public record PriceCard(String productCode, String productLabel, List<PriceRow> rows, Instant retrievedAt) {}

    public record PriceRow(String retailer, BigDecimal price, boolean lowest) {}

    private record SearchProduct(@JsonProperty("product_code") String productCode, String label, String category,
                                 @JsonProperty("lowest_listed_price") BigDecimal lowestPrice) {}

    private record SearchReply(List<SearchProduct> products, @JsonProperty("total_matches") int totalMatches,
                               @JsonProperty("sorted_by") String sortedBy, Source source) {}

    private record PriceReply(@JsonProperty("product_code") String productCode, String label,
                              List<PriceRow> prices, Source source) {}

    private record Source(String origin, @JsonProperty("source_url") String sourceUrl,
                          @JsonProperty("retrieved_at") String retrievedAt,
                          @JsonProperty("source_price_date") String sourcePriceDate) {}

    private static final class InvalidArguments extends RuntimeException {
        private InvalidArguments(String message) {
            super(message);
        }
    }
}
