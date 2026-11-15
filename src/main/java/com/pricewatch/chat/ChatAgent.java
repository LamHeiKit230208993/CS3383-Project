package com.pricewatch.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.ProductView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Short reasoning loop over the accepted catalogue. The model may request read-only
 * tools ({@code search_products}, {@code product_prices}); this class runs them
 * deterministically through {@link CatalogService} and feeds the results back. The
 * model never sees SQL or the database, and every price it can quote comes from a
 * tool result, so it cannot invent one.
 */
class ChatAgent {
    private static final Logger log = LoggerFactory.getLogger(ChatAgent.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_STEPS = 4;
    private static final int MAX_SEARCH_HITS = 5;

    static final String SYSTEM = """
            You are the PriceWatch price assistant for a local Hong Kong price-watch app.
            Answer the shopper's question in natural language. You have tools to look up the
            accepted catalogue:
            - search_products(query): find products by name/brand/keywords
            - product_prices(code): get the retailer prices for one product code
            Rules:
            - Always look up facts with the tools before answering. Do not rely on memory.
            - Use only product codes and prices returned by the tools. Never invent a price,
              retailer, stock level, pack size, date, previous price, or future price.
            - If the question compares products, look each one up and point out the cheapest
              using the listed amounts.
            - If nothing matches, say so and suggest searching Discover. If it is genuinely
              ambiguous, ask the shopper to choose.
            - Quote amounts as given, e.g. HK$7.50. Keep the answer to 1-3 short sentences.
            - This is not a purchase and not an actual-spend claim.
            """;

    static final List<ToolSpec> TOOLS = List.of(
            new ToolSpec("search_products", "Find products in the accepted catalogue by free-text name, brand, or category.",
                    Map.of("type", "object",
                            "properties", Map.of("query", Map.of("type", "string",
                                    "description", "Words describing the product, e.g. 'milk' or 'garden bread'")),
                            "required", List.of("query"))),
            new ToolSpec("product_prices", "Get each retailer's listed price for one product code.",
                    Map.of("type", "object",
                            "properties", Map.of("code", Map.of("type", "string",
                                    "description", "A product code returned by search_products")),
                            "required", List.of("code"))));

    private final CatalogService catalog;
    private final AiClient ai;

    ChatAgent(CatalogService catalog, AiClient ai) {
        this.catalog = catalog;
        this.ai = ai;
    }

    /** Runs the loop and returns the final assistant text plus the facts it used. */
    ChatReply run(String question) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(SYSTEM));
        messages.add(ChatMessage.user(question));
        StringBuilder facts = new StringBuilder();

        for (int step = 0; step < MAX_STEPS; step++) {
            ChatCompletion turn = ai.completeTurn(messages, TOOLS);
            if (!turn.wantsTools()) {
                return ChatReply.answer(turn.text(), facts.toString().strip(), true);
            }
            messages.add(ChatMessage.assistant(turn.text(), turn.toolCalls()));
            for (ToolCall call : turn.toolCalls()) {
                String result = execute(call);
                if (call.name().equals("product_prices") && result.startsWith("Product:")) {
                    facts.append(result).append("\n\n");
                }
                messages.add(ChatMessage.tool(call.id(), result));
            }
        }

        // Safety stop: ask the model to answer with what it has, without more tools.
        ChatCompletion finalTurn = ai.completeTurn(messages, null);
        if (finalTurn.text().isBlank()) {
            throw new AiUnavailableException("The AI did not produce an answer within the step limit.");
        }
        return ChatReply.answer(finalTurn.text(), facts.toString().strip(), true);
    }

    private String execute(ToolCall call) {
        try {
            JsonNode args = JSON.readTree(call.argumentsJson());
            return switch (call.name()) {
                case "search_products" -> search(args.path("query").asText(""));
                case "product_prices" -> prices(args.path("code").asText(""));
                default -> "Unknown tool: " + call.name();
            };
        } catch (Exception exception) {
            log.warn("Tool call failed: {}", call.name(), exception);
            return "Tool error: could not run " + call.name() + ".";
        }
    }

    private String search(String query) {
        var results = catalog.searchResults(query, "");
        List<ProductView> hits = results.matches().isEmpty() ? results.related() : results.matches();
        if (hits.isEmpty()) {
            return "No products matched '" + query + "'.";
        }
        return hits.stream().limit(MAX_SEARCH_HITS)
                .map(p -> p.code() + " | " + nullSafe(p.name().en()) + " | " + nullSafe(p.brand().en())
                        + (p.lowestPrice() == null ? "" : " | lowest " + p.lowestPrice().toPlainString() + " HKD"))
                .collect(Collectors.joining("\n"));
    }

    private String prices(String code) {
        try {
            return facts(catalog.find(code));
        } catch (IllegalArgumentException notFound) {
            return "No product with code " + code + ".";
        }
    }

    static String facts(ProductView product) {
        StringBuilder facts = new StringBuilder();
        facts.append("Product: ").append(nullSafe(product.name().en()))
                .append(" (code ").append(product.code()).append(")\n");
        if (product.lowestPrice() == null) {
            facts.append("Lowest listed price: unknown (no retailer lists a price)\n");
        } else {
            facts.append("Lowest listed price: ").append(product.lowestPrice().toPlainString()).append(" HKD at ");
            facts.append(product.lowestListings().stream().map(ListingView::retailerCode)
                    .collect(Collectors.joining(", "))).append("\n");
        }
        for (ListingView listing : product.listings()) {
            facts.append("Retailer ").append(listing.retailerCode()).append(": ");
            facts.append(listing.price() == null ? "no current price" : listing.price().toPlainString() + " HKD");
            if (!listing.offers().isEmpty()) {
                facts.append(" (offers: ").append(listing.offers().stream()
                        .map(offer -> offer.en()).filter(v -> v != null && !v.isBlank())
                        .collect(Collectors.joining("; "))).append(")");
            }
            facts.append("\n");
        }
        return facts.toString().strip();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
