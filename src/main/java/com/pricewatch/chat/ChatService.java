package com.pricewatch.chat;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.ProductView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * J09: answers price questions in natural language with a short tool-calling agent
 * loop. The model reasons and asks for lookups; the app runs them deterministically
 * against the accepted catalogue and the model only phrases answers from returned
 * facts, so no price is invented. When the AI is unavailable a deterministic fallback
 * answers from checked records so search and comparison keep working.
 */
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_QUESTION = 500;

    private final CatalogService catalog;
    private final AiClient ai;
    private final ChatAgent agent;

    public ChatService(CatalogService catalog, AiClient ai) {
        this.catalog = catalog;
        this.ai = ai;
        this.agent = new ChatAgent(catalog, ai);
    }

    public ChatReply reply(String question) {
        String q = question == null ? "" : question.strip();
        if (q.isEmpty()) {
            return ChatReply.clarify("Type a price question, for example: Which shop has the cheapest milk?");
        }
        if (q.length() > MAX_QUESTION) {
            return ChatReply.clarify("Keep the question under " + MAX_QUESTION + " characters.");
        }
        if (catalog.catalogue().isEmpty()) {
            return ChatReply.clarify("No prices are loaded yet. Load the checked sample or refresh live prices first.");
        }
        if (!ai.isConfigured()) {
            return fallback(q);
        }
        try {
            return agent.run(q);
        } catch (AiUnavailableException exception) {
            log.warn("AI answer unavailable; returning checked records", exception);
            return fallback(q);
        }
    }

    /** Deterministic answer used when the AI is unavailable or returns nothing usable. */
    private ChatReply fallback(String question) {
        var results = catalog.searchResults(question, "");
        List<ProductView> found = results.matches().isEmpty() ? results.related() : results.matches();
        if (found.isEmpty()) {
            return ChatReply.clarify("The AI assistant is unavailable and I could not match that product. "
                    + "Try another name, or search Discover.");
        }
        List<ProductView> chosen = found.size() > 1 ? found.subList(0, Math.min(3, found.size())) : found;
        String facts = chosen.stream().map(ChatAgent::facts).collect(Collectors.joining("\n\n"));
        String message = chosen.stream().map(ChatService::localAnswer).collect(Collectors.joining(" "));
        return ChatReply.answer(message
                + " (AI assistant unavailable, so this answer is shown from checked records only.)",
                facts, false);
    }

    private static String localAnswer(ProductView product) {
        if (product.lowestPrice() == null) {
            return "The checked catalogue has no listed price for " + label(product) + " right now.";
        }
        String retailers = product.lowestListings().stream().map(ListingView::retailerCode)
                .collect(Collectors.joining(" and "));
        return "The lowest listed price for " + label(product) + " is HK$"
                + product.lowestPrice().toPlainString() + " at " + retailers + ".";
    }

    private static String label(ProductView product) {
        String name = product.name().en();
        return name == null || name.isBlank() ? product.code() : name;
    }
}
