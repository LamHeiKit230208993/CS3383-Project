package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.ProductView;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Controller
public class AskController {
    private static final String HISTORY = "pricewatch.chatHistory";
    private static final int HISTORY_LIMIT = 12;
    private static final Pattern SAME_PACK_COMPARISON = Pattern.compile(
            "(?:which (?:store|retailer)(?: is (?:the )?cheapest)?|which is (?:the )?cheapest|cheapest|lowest price"
                    + "|how much is (?:it|this|that|the same pack)|how much does (?:it|this|that) cost)");
    private static final Pattern RETAILER_FOLLOW_UP_PREFIX = Pattern.compile(
            "^(?:and(?: at)? |at |(?:how much|price|what is the price) at )");
    private final CatalogService catalog;
    private final ChatIntentClient intents;
    private final HelloController format;
    private final MessageSource messages;

    public AskController(CatalogService catalog, ChatIntentClient intents, HelloController format, MessageSource messages) {
        this.catalog = catalog;
        this.intents = intents;
        this.format = format;
        this.messages = messages;
    }

    @GetMapping("/ask")
    String show(@RequestParam(defaultValue = "") String chatId, Model model, HttpSession session) {
        var chat = conversation(session, chatId);
        model.addAttribute("scene", "ask");
        model.addAttribute("title", "Ask about prices");
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("messages", chat.messages());
        model.addAttribute("candidates", chat.candidates());
        model.addAttribute("chatId", chat.id());
        model.addAttribute("chatTitle", chat.title().isBlank() ? text("chat.newTitle") : chat.title());
        model.addAttribute("chatHistory", history(session));
        model.addAttribute("awaitingReply", chat.state() != TurnState.READY);
        return "market-desk";
    }

    @PostMapping("/ask")
    String ask(@RequestParam(defaultValue = "") String question,
               @RequestParam(defaultValue = "") String chatId, HttpSession session) {
        return replyToTurn(acceptQuestion(question, chatId, session).id(), session);
    }

    @PostMapping("/ask/send")
    String send(@RequestParam(defaultValue = "") String question,
                @RequestParam(defaultValue = "") String chatId, HttpSession session) {
        return redirect(acceptQuestion(question, chatId, session));
    }

    private Conversation acceptQuestion(String question, String chatId, HttpSession session) {
        synchronized (session) {
            var chat = conversation(session, chatId);
            if (chat.state() != TurnState.READY) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
            if (chat.id().isBlank()) {
                chat = new Conversation(UUID.randomUUID().toString(), title(question), null, List.of(), "",
                        List.of(), TurnState.READY);
            }
            var accepted = question.isBlank() || question.length() > 240
                    ? chat.append(chat.productCode(), chat.candidates(), chat.requestedRetailer(),
                            TurnState.READY, reply("chat.invalid"))
                    : chat.append(chat.productCode(), chat.candidates(), chat.requestedRetailer(),
                            TurnState.WAITING, new Message(true, question.strip()));
            store(session, accepted);
            return accepted;
        }
    }

    @PostMapping("/ask/reply")
    String replyToTurn(@RequestParam(defaultValue = "") String chatId, HttpSession session) {
        Conversation claimed;
        synchronized (session) {
            var chat = conversation(session, chatId);
            if (chat.state() != TurnState.WAITING) return redirect(chat);
            claimed = chat.withState(TurnState.REPLYING);
            store(session, claimed);
        }

        Conversation result = null;
        try {
            result = resolve(claimed);
        } finally {
            synchronized (session) {
                // A reset or an evicted chat must not be revived by a late reply.
                if (history(session).contains(claimed)) {
                    store(session, result == null ? claimed.withState(TurnState.WAITING) : result);
                }
            }
        }
        return redirect(claimed);
    }

    private Conversation resolve(Conversation chat) {
        String question = chat.messages().getLast().text();
        String normalized = normalize(question);
        String conversationalKey = conversationalKey(normalized);
        if (conversationalKey != null) {
            return respond(chat, chat.productCode(), chat.candidates(), reply(conversationalKey));
        }
        String query = normalized
                .replaceFirst("^(?:how much (?:is|are) |what(?: is|'s) the price of |where (?:is|are|can i buy) |cheapest price for |what about |how about )", "")
                .replaceFirst(" (?:cheapest|the cheapest)$", "").strip();
        ProductView context = activeProduct(chat.productCode());
        if (context != null) {
            String requestedRetailer = RETAILER_FOLLOW_UP_PREFIX.matcher(query).replaceFirst("");
            var listing = context.listings().stream()
                    .filter(candidate -> retailerKey(candidate.retailerCode()).equals(retailerKey(requestedRetailer)))
                    .findFirst();
            if (listing.isPresent()) {
                return respond(chat, context.code(), List.of(),
                        reply("chat.at", label(context), describe(listing.get())));
            }
            if (SAME_PACK_COMPARISON.matcher(normalized).matches()) {
                return respond(chat, context.code(), List.of(), answer(context, "", false));
            }
        }
        var recent = chat.messages().subList(Math.max(0, chat.messages().size() - 9), chat.messages().size() - 1)
                .stream()
                .map(message -> (message.shopper() ? "Shopper: " : "Market Desk: ")
                        + message.text().substring(0, Math.min(480, message.text().length())))
                .toList();
        var intent = intents.interpret(question, context == null ? "" : label(context),
                context == null ? List.of() : context.listings().stream().map(ListingView::retailerCode).toList(), recent);
        if (intent.isPresent()) {
            var request = intent.get();
            return switch (request.action()) {
                case "search" -> search(chat, context, normalize(request.query()), request.retailer(), false);
                case "compare" -> respond(chat, context.code(), List.of(),
                        answer(context, request.retailer(), false));
                case "clarify" -> respond(chat, chat.productCode(), chat.candidates(),
                        reply("chat.clarify." + request.field()));
                case "converse" -> respond(chat, chat.productCode(), chat.candidates(),
                        reply("chat.conversation." + request.field()));
                default -> throw new IllegalStateException("Unsupported chat action");
            };
        }
        return search(chat, context, query, "", true);
    }

    private Conversation search(Conversation chat, ProductView context, String query,
                                String retailer, boolean localFallback) {
        List<ProductView> matches = query.isBlank() ? List.of() : catalog.searchResults(query, "").matches();
        var exact = matches.stream().filter(p -> p.code().equalsIgnoreCase(query)).findFirst();
        if (exact.isPresent()) matches = List.of(exact.get());
        if (matches.size() == 1) {
            var product = matches.getFirst();
            return respond(chat, product.code(), List.of(), answer(product, retailer, localFallback));
        } else if (matches.isEmpty()) {
            Message response = context != null ? reply("chat.noMatchWithContext", label(context))
                    : chat.candidates().isEmpty() ? reply("chat.noMatch") : reply("chat.chooseReply");
            return respond(chat, context == null ? null : context.code(), chat.candidates(), response);
        } else {
            var choices = matches.stream().limit(20)
                    .map(p -> new Choice(p.code(), label(p) + " · " + p.code())).toList();
            return chat.append(null, choices, retailer, TurnState.READY, reply("chat.chooseReply"));
        }
    }

    @PostMapping("/ask/choose")
    String choose(@RequestParam String code, @RequestParam String chatId, HttpSession session) {
        synchronized (session) {
            var chat = conversation(session, chatId);
            if (chat.state() != TurnState.READY ||
                    chat.candidates().stream().noneMatch(choice -> choice.code().equals(code))) {
                return redirect(chat);
            }
            var product = activeProduct(code);
            var completed = product == null
                    ? respond(chat, null, List.of(), reply("chat.removed"))
                    : respond(chat, code, List.of(), new Message(true, label(product)),
                            answer(product, chat.requestedRetailer(), false));
            store(session, completed);
            return redirect(chat);
        }
    }

    private static String redirect(Conversation chat) {
        return chat.id().isBlank() ? "redirect:/ask" : "redirect:/ask?chatId=" + chat.id();
    }

    private ProductView activeProduct(String code) {
        if (code == null) return null;
        try {
            var product = catalog.find(code);
            return product.active() ? product : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Message answer(ProductView product, String retailer, boolean localFallback) {
        if (!retailer.isBlank()) {
            var listing = product.listings().stream()
                    .filter(candidate -> retailerKey(candidate.retailerCode()).equals(retailerKey(retailer)))
                    .findFirst();
            return listing.map(value -> reply("chat.at", label(product), describe(value)))
                    .orElseGet(() -> reply("chat.noRetailer",
                            String.join(", ", product.listings().stream().map(ListingView::retailerCode).toList())));
        }
        String prices = String.join("\n", product.listings().stream()
                .map(listing -> describe(listing)
                        + (product.lowestListings().contains(listing) ? " · " + text("chat.lowest") : ""))
                .toList());
        String reason = text(product.lowestPrice() == null ? "chat.noPrice"
                : product.lowestListings().size() > 1 ? "chat.tied" : "chat.minimum");
        String fallback = localFallback ? "\n" + text("chat.localFallback") : "";
        return new Message(false, label(product) + "\n" + prices + "\n" + reason + fallback);
    }

    private static String retailerKey(String text) {
        return normalize(text).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private String describe(ListingView listing) {
        return listing.retailerCode() + " · " + (listing.price() == null ? text("chat.unknown") : format.money(listing.price()));
    }

    private String label(ProductView product) {
        var locale = LocaleContextHolder.getLocale();
        String brand = product.brand().forLocale(locale);
        return (brand.isBlank() ? "" : brand + " · ") + product.name().forLocale(locale);
    }

    private Message reply(String key, Object... arguments) {
        return new Message(false, text(key, arguments));
    }

    private String text(String key, Object... arguments) {
        return messages.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace('’', '\'').replace("what's ", "what is ").replaceAll("(?<=\\p{L})'s\\b", "")
                .replaceAll("[?!？,，]+", " ").replaceAll("\\s+", " ").strip();
    }

    private static String conversationalKey(String normalized) {
        return switch (normalized.replaceFirst("[.。]+$", "")) {
            case "hi", "hey", "hello", "hi there", "hey there", "你好", "您好", "嗨" ->
                    "chat.conversation.greeting";
            case "who are you", "who are u", "what are you", "what can you do", "what can u do", "help",
                 "你是誰", "你是谁", "你能做什麼", "你能做什么", "你可以做甚麼", "你可以做什么" ->
                    "chat.conversation.identity";
            case "thanks", "thank you", "thx", "thanks a lot", "謝謝", "谢谢", "多謝", "多谢" ->
                    "chat.conversation.thanks";
            default -> null;
        };
    }

    static List<Conversation> history(HttpSession session) {
        try {
            return session != null && session.getAttribute(HISTORY) instanceof ChatHistory history
                    ? history.chats() : List.of();
        } catch (IllegalStateException expired) {
            return List.of();
        }
    }

    private static Conversation conversation(HttpSession session, String id) {
        return history(session).stream().filter(chat -> chat.id().equals(id)).findFirst()
                .orElseGet(() -> new Conversation("", "", null, List.of(), "", List.of(), TurnState.READY));
    }

    private static String title(String question) {
        String first = question.strip().replaceAll("\\s+", " ");
        return first.codePointCount(0, first.length()) > 64
                ? first.substring(0, first.offsetByCodePoints(0, 64)) + "…" : first;
    }

    private static Conversation respond(Conversation chat, String code, List<Choice> choices, Message... messages) {
        return chat.append(code, choices, choices.isEmpty() ? "" : chat.requestedRetailer(), TurnState.READY, messages);
    }

    private static void store(HttpSession session, Conversation updated) {
        var chats = new ArrayList<Conversation>();
        chats.add(updated);
        history(session).stream().filter(previous -> !previous.id().equals(updated.id())).forEach(chats::add);
        if (chats.size() > HISTORY_LIMIT) {
            for (int index = chats.size() - 1; index > 0; index--) {
                if (chats.get(index).state() == TurnState.READY) {
                    chats.remove(index);
                    session.setAttribute(HISTORY, new ChatHistory(List.copyOf(chats)));
                    return;
                }
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        session.setAttribute(HISTORY, new ChatHistory(List.copyOf(chats)));
    }

    public record Message(boolean shopper, String text) {}
    public record Choice(String code, String label) {}
    public record Conversation(String id, String title, String productCode, List<Choice> candidates,
                               String requestedRetailer, List<Message> messages, TurnState state) {
        private Conversation withState(TurnState nextState) {
            return new Conversation(id, title, productCode, candidates, requestedRetailer, messages, nextState);
        }

        private Conversation append(String code, List<Choice> choices, String retailer, TurnState nextState,
                                    Message... additions) {
            var transcript = new ArrayList<>(messages);
            transcript.addAll(List.of(additions));
            return new Conversation(id, title, code, choices, retailer,
                    List.copyOf(transcript.subList(Math.max(0, transcript.size() - 24), transcript.size())), nextState);
        }
    }
    private enum TurnState { READY, WAITING, REPLYING }
    private record ChatHistory(List<Conversation> chats) {}
}
