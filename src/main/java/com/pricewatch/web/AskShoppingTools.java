package com.pricewatch.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.pricewatch.budget.BudgetService;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.ProductTargetService;
import com.pricewatch.notifications.WatchRepeat;
import com.pricewatch.notifications.WatchRuleService;
import com.pricewatch.notifications.WatchRuleView;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import com.pricewatch.tracking.TrackingService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Shopping tools collect evidence and proposed changes. Only the controller can apply a proposal. */
@Service
public class AskShoppingTools {
    private static final int MAX_ROWS = 20;
    private static final int MAX_CHANGES = 5;
    private static final ZoneId HONG_KONG = ZoneId.of("Asia/Hong_Kong");
    private static final Map<String, Object> PRODUCT_CODE = string(
            "Exact product code from search or a saved list. Omit to use the already selected pack.");
    private static final Map<String, Object> LIST_ID = integer("Exact saved list ID returned by list_carts.");
    private static final Map<String, Object> LIST_NAME = string("Exact list name. Omit both list fields for the main Cart.");

    static final List<DeepSeekChatClient.Tool> TOOLS = List.of(
            tool("list_carts", "Find the main Cart and locally saved list names and IDs.", Map.of()),
            tool("get_cart", "Read the saved items, quantities, prices, total and budget for one list.",
                    Map.of("list_id", LIST_ID, "list_name", LIST_NAME)),
            tool("get_wishlist", "Read the saved Wishlist and exact product codes.", Map.of()),
            tool("get_alerts", "Show saved rules or recent history. Read rule IDs before changing rules.",
                    Map.of("view", choice("rules", "history"), "product_code", PRODUCT_CODE)),
            tool("calculate", "Calculate decimal arithmetic with + - * / parentheses and percentages, for example 15% of 80.",
                    Map.of("expression", string("Arithmetic expression."), "currency", bool("Display the result in HKD.")),
                    "expression"),
            tool("propose_cart", "Prepare a cart write for an explicit shopper request. Removes and deleting lists need confirmation.",
                    fields("operation", choice("add", "remove", "create_list", "rename_list", "delete_list"),
                            "list_id", LIST_ID, "list_name", LIST_NAME, "product_code", PRODUCT_CODE,
                    "quantity", integer("Packs to add or remove. Defaults to one for add and the whole item for remove."),
                            "retailer", string("Exact retailer code or automatic. Omit to retain an existing retailer."),
                            "new_name", string("Name for a new or renamed list.")), "operation"),
            tool("propose_wishlist", "Prepare saving or removing an exact pack when the shopper requests it.",
                    Map.of("operation", choice("add", "remove"), "product_code", PRODUCT_CODE), "operation"),
            tool("propose_alert", """
                    Prepare an alert or reminder change for confirmation. For create/update of a watch rule, send only
                    the facts supplied by the shopper. The tool retains incomplete drafts and reports missing fields.
                    For an existing rule, call get_alerts to select the rule ID. Read draft before changing a pending setup.
                    """, fields("operation", choice("create", "update", "toggle", "delete", "draft", "discard_draft"),
                    "rule_id", string("Opaque rule_id from get_alerts."), "product_code", PRODUCT_CODE,
                    "condition", choice("price", "calendar", "combined"),
                    "target", string("Positive or zero HKD price target as a decimal."),
                    "start_at", string("Shopper-supplied Hong Kong date and time as YYYY-MM-DDTHH:mm."),
                    "repeat", choice("ONCE", "DAILY", "WEEKLY", "FORTNIGHTLY", "MONTHLY"),
                    "enabled", bool("true to resume; false to pause."),
                    "quantity", integer("Number of packs for an existing quantity reminder."),
                    "interval_days", integer("Interval for an existing interval reminder or combined rule."),
                    "retailer", string("Exact retailer code for an existing quantity reminder.")), "operation"));

    private final CatalogService catalogue;
    private final ShoppingService shopping;
    private final TrackingService tracking;
    private final WatchRuleService watches;
    private final ProductTargetService targets;
    private final ReminderService reminders;
    private final AlertService alerts;
    private final BudgetService budgets;
    private final MessageSource messages;
    private final Clock clock;

    AskShoppingTools(CatalogService catalogue, ShoppingService shopping, TrackingService tracking,
                     WatchRuleService watches, ProductTargetService targets, ReminderService reminders,
                     AlertService alerts, BudgetService budgets, MessageSource messages, Clock clock) {
        this.catalogue = catalogue;
        this.shopping = shopping;
        this.tracking = tracking;
        this.watches = watches;
        this.targets = targets;
        this.reminders = reminders;
        this.alerts = alerts;
        this.budgets = budgets;
        this.messages = messages;
        this.clock = clock;
    }

    Turn open(Locale locale, String selectedCode, Draft draft) {
        return new Turn(locale, selectedCode, draft);
    }

    final class Turn {
        private final Locale locale;
        private final List<Card> cards = new ArrayList<>();
        private final Map<String, Operation> operations = new LinkedHashMap<>();
        private String selectedCode;
        private Draft draft;

        private Turn(Locale locale, String selectedCode, Draft draft) {
            this.locale = locale;
            this.selectedCode = selectedCode;
            this.draft = draft;
        }

        Object execute(String name, JsonNode args) {
            if (args == null || !args.isObject()) return Map.of("error", "Tool arguments must be an object.");
            try {
                return switch (name) {
                    case "list_carts" -> readCarts();
                    case "get_cart" -> readCart(cart(args));
                    case "get_wishlist" -> readWishlist();
                    case "get_alerts" -> readAlerts(args);
                    case "calculate" -> calculate(args);
                    case "propose_cart" -> proposeCart(args);
                    case "propose_wishlist" -> proposeWishlist(args);
                    case "propose_alert" -> proposeAlert(args);
                    default -> Map.of("error", "Unknown shopping tool.");
                };
            } catch (IllegalArgumentException failure) {
                return Map.of("error", Objects.toString(failure.getMessage(), "Invalid shopping request."));
            }
        }

        List<Card> cards() { return List.copyOf(cards); }
        List<Operation> operations() { return List.copyOf(operations.values()); }
        Draft draft() { return draft; }
        String selectedCode() { return selectedCode; }
        void selectFromCatalogue(String code) { selectedCode = code; }

        private void card(Card card) {
            if (cards.size() < 10 && !cards.contains(card)) cards.add(card);
        }

        private Object readCarts() {
            var lists = carts(locale);
            card(new Card("lists", ui(locale, "lists"), null, lists.stream().limit(MAX_ROWS)
                    .map(list -> new Row(list.name(), null, null, list.id())).toList(), null));
            return Map.of("lists", lists.stream().limit(MAX_ROWS).toList(), "total", lists.size());
        }

        private Object readCart(CartRef list) {
            var items = shopping.items(list.id());
            var summary = Summary.from(items);
            var budget = budgets.forSummary(summary);
            var known = money(summary.knownSubtotal());
            var details = new ArrayList<String>();
            details.add(ui(locale, summary.unknownItemCount() > 0 ? "subtotal" : "total", known));
            if (summary.unknownItemCount() > 0) details.add(ui(locale, "unknownItems", summary.unknownItemCount()));
            if (budget.limitAmount() != null) {
                details.add(ui(locale, "budget", money(budget.limitAmount())));
                if (summary.unknownItemCount() == 0) details.add(ui(locale, "balance", money(budget.balance())));
            }
            if (items.size() > MAX_ROWS) details.add(ui(locale, "more", items.size() - MAX_ROWS));
            var visible = items.stream().limit(MAX_ROWS)
                    .map(item -> new CartRow(item.product().code(), label(item.product(), locale), item.quantity(),
                            item.retailerCode(), item.selectedRetailerCode(), item.unitPrice(), item.lineTotal(), item.checked()))
                    .toList();
            card(new Card("cart", list.name(), String.join(" · ", details), visible.stream()
                    .map(item -> new Row(item.label(), ui(locale, "packs", item.quantity()) + " · "
                            + (item.retailer() == null ? "" : item.retailer() + " · ")
                            + (item.total() == null ? ui(locale, "unknown") : money(item.total()))
                            + (item.checked() ? " · " + ui(locale, "checked") : ""), item.productCode())).toList(), list.id()));
            return new CartReply(list, visible, summary.knownSubtotal(), summary.unknownItemCount(),
                    budget.period().name(), budget.limitAmount(), items.size());
        }

        private Object readWishlist() {
            var saved = tracking.savedProducts();
            var products = saved.stream().limit(MAX_ROWS).map(item -> {
                var product = item.product();
                return new SavedRow(product.code(), label(product, locale), product.lowestPrice());
            }).toList();
            card(new Card("wishlist", ui(locale, "wishlist"),
                    saved.size() > MAX_ROWS ? ui(locale, "more", saved.size() - MAX_ROWS) : null,
                    products.stream().map(item -> new Row(item.label(),
                            item.lowestPrice() == null ? ui(locale, "unknown") : money(item.lowestPrice()), item.productCode()))
                            .toList(), null));
            return Map.of("items", products, "total", saved.size());
        }

        private Object readAlerts(JsonNode args) {
            String view = arg(args, "view", 16);
            String code = arg(args, "product_code", 80);
            if ("history".equals(view)) {
                var events = alertHistory(locale, code);
                card(new Card("alert-history", ui(locale, "history"), null,
                        events.stream().map(item -> new Row(item.label(), item.detail(), item.productCode())).toList(), null));
                return Map.of("events", events.stream().map(event -> Map.of(
                        "product_code", event.productCode(), "label", event.label(), "detail", event.detail(),
                        "created_at", event.createdAt().toString())).toList());
            }
            if (view != null && !"rules".equals(view)) throw invalid("Choose rules or history.");
            var rules = configuredRules(locale).stream()
                    .filter(rule -> code == null || code.equals(rule.productCode())).toList();
            card(new Card("alerts", ui(locale, "alerts"), rules.size() > MAX_ROWS ? ui(locale, "more", rules.size() - MAX_ROWS) : null,
                    rules.stream().limit(MAX_ROWS).map(item -> new Row(item.label(), item.detail(), item.productCode()))
                            .toList(), null));
            return Map.of("rules", rules.stream().limit(MAX_ROWS).map(AskShoppingTools::ruleDetails).toList(),
                    "total", rules.size());
        }

        private Object calculate(JsonNode args) {
            String expression = required(args, "expression", 120);
            var result = AskArithmetic.calculate(expression);
            String display = Boolean.TRUE.equals(flag(args, "currency"))
                    ? money(result.setScale(2, RoundingMode.HALF_UP)) : result.toPlainString();
            card(new Card("calculation", ui(locale, "calculation"), expression + " = " + display, List.of(), null));
            return Map.of("value", result.toPlainString(), "display", display);
        }

        private Object proposeCart(JsonNode args) {
            String action = required(args, "operation", 16);
            if (action.equals("create_list")) {
                return propose(new Change(Kind.CREATE_LIST, null, null, null, null,
                        required(args, "new_name", 120), null, null));
            }
            var list = cart(args);
            return switch (action) {
                case "rename_list" -> propose(new Change(Kind.RENAME_LIST, requireNamedList(list), null, null,
                        null, required(args, "new_name", 120), null, null));
                case "delete_list" -> propose(new Change(Kind.DELETE_LIST, requireNamedList(list), null, null,
                        null, null, null, null));
                case "add", "remove" -> {
                    String code = productCode(args);
                    Integer quantity = optionalQuantity(number(args, "quantity"));
                    if (quantity == null && action.equals("add")) quantity = 1;
                    yield propose(new Change(action.equals("add") ? Kind.ADD_CART : Kind.REMOVE_CART, list.id(), code,
                            quantity, arg(args, "retailer", 80), null, null, null));
                }
                default -> throw invalid("Choose a supported cart operation.");
            };
        }

        private Object proposeWishlist(JsonNode args) {
            Kind action = switch (required(args, "operation", 16)) {
                case "add" -> Kind.ADD_WISHLIST;
                case "remove" -> Kind.REMOVE_WISHLIST;
                default -> throw invalid("Choose add or remove.");
            };
            return propose(new Change(action, null, productCode(args), null, null, null, null, null));
        }

        private Object proposeAlert(JsonNode args) {
            String action = required(args, "operation", 20);
            if (action.equals("discard_draft")) {
                draft = null;
                return Map.of("status", "draft_discarded");
            }
            if (action.equals("draft")) return draft == null ? Map.of("status", "no_draft") : draftStatus(draft);
            if (action.equals("create")) return draftAlert(args);
            String ruleId = required(args, "rule_id", 105);
            var rule = resolveRule(ruleId, locale);
            if (action.equals("delete")) {
                return propose(new Change(Kind.DELETE_ALERT, null, rule.productCode(), null, null, null, ruleId, null));
            }
            if (action.equals("toggle")) {
                Boolean enabled = flag(args, "enabled");
                if (enabled == null) throw invalid("Specify whether to enable or pause the rule.");
                return propose(new Change(Kind.TOGGLE_ALERT, null, rule.productCode(), null, null, null, ruleId,
                        new AlertChange(null, null, null, null, enabled)));
            }
            if (!action.equals("update")) throw invalid("Choose a supported alert operation.");
            return propose(new Change(Kind.UPDATE_ALERT, null, rule.productCode(),
                    optionalQuantity(number(args, "quantity")), arg(args, "retailer", 80), null, ruleId,
                    new AlertChange(optionalPrice(args), optionalDate(args), optionalRepeat(args),
                            optionalInterval(number(args, "interval_days")), null)));
        }

        private Object draftAlert(JsonNode args) {
            String requestedCode = arg(args, "product_code", 80);
            String code = requestedCode != null ? requestedCode
                    : draft != null && draft.productCode() != null ? draft.productCode() : selectedCode;
            if (code != null) {
                activeProduct(code);
                selectedCode = code;
            }
            if (draft != null && code != null && !Objects.equals(code, draft.productCode()) && draft.productCode() != null) {
                draft = null;
            }
            String condition = arg(args, "condition", 16);
            String type = condition == null && draft != null ? draft.condition() : condition;
            if (type != null && !List.of("price", "calendar", "combined").contains(type)) {
                throw invalid("Choose price, calendar, or combined.");
            }
            var target = optionalPrice(args);
            var date = optionalDate(args);
            var repeat = optionalRepeat(args);
            if (draft != null) {
                if (target == null) target = draft.target();
                if (date == null) date = draft.startAt();
                if (repeat == null) repeat = draft.repeat();
            }
            draft = new Draft(code, type, "calendar".equals(type) ? null : target,
                    "price".equals(type) ? null : date, repeat);
            var missing = missing(draft);
            if (!missing.isEmpty()) return draftStatus(draft);
            var result = propose(new Change(Kind.CREATE_ALERT, null, code, null, null, null, null,
                    new AlertChange(draft.target(), draft.startAt(), draft.repeat(), null, null)));
            draft = null;
            return result;
        }

        private Object propose(Change request) {
            var operation = prepare(request, locale);
            String key = operation.conflictKey();
            var previous = operations.get(key);
            if (previous != null && !previous.change().equals(operation.change())) {
                throw invalid("Only one change to the same item or rule can be prepared in a reply.");
            }
            if (previous == null && operations.size() >= MAX_CHANGES) {
                throw invalid("Up to five changes can be prepared in a reply.");
            }
            operations.putIfAbsent(key, operation);
            if (request.productCode() != null) selectedCode = request.productCode();
            return Map.of("status", operation.requiresConfirmation() ? "awaiting_confirmation" : "prepared",
                    "change", operation.preview(), "saved", false);
        }

        private CartRef cart(JsonNode args) {
            var id = number(args, "list_id");
            String name = arg(args, "list_name", 120);
            var lists = carts(locale);
            if (id != null) {
                if (id <= 0) throw invalid("Choose a valid list ID.");
                var list = lists.stream().filter(item -> id.equals(item.id())).findFirst()
                        .orElseThrow(() -> invalid("The saved list could not be found. Use list_carts."));
                if (name == null || list.name().equalsIgnoreCase(name)) return list;
                throw invalid("The list name and ID do not refer to the same list.");
            }
            if (name == null || "Cart".equalsIgnoreCase(name) || "My list".equalsIgnoreCase(name)) return lists.getFirst();
            var matches = lists.stream().filter(item -> item.name().equalsIgnoreCase(name)).toList();
            if (matches.size() != 1) throw invalid("Use list_carts and ask the shopper to choose the exact saved list.");
            return matches.getFirst();
        }

        private String productCode(JsonNode args) {
            var code = arg(args, "product_code", 80);
            if (code == null) code = selectedCode;
            if (code == null) throw invalid("Choose an exact product pack first.");
            return code;
        }
    }

    private static Object draftStatus(Draft draft) {
        var details = new LinkedHashMap<String, Object>();
        details.put("product_code", draft.productCode());
        details.put("condition", draft.condition());
        details.put("target", draft.target());
        details.put("start_at", stringDate(draft.startAt()));
        details.put("repeat", draft.repeat());
        return Map.of("status", "draft", "draft", details, "missing", missing(draft));
    }

    private static List<String> missing(Draft draft) {
        var missing = new ArrayList<String>();
        if (draft.productCode() == null) missing.add("exact product pack");
        if (draft.condition() == null) missing.add("price condition or calendar time");
        if (List.of("price", "combined").contains(Objects.toString(draft.condition(), "")) && draft.target() == null) {
            missing.add("price target");
        }
        if (List.of("calendar", "combined").contains(Objects.toString(draft.condition(), "")) && draft.startAt() == null) {
            missing.add("date and time in Hong Kong");
        }
        if (draft.repeat() == null) missing.add("repeat schedule or one-time");
        return List.copyOf(missing);
    }

    Operation refresh(Operation operation, Locale locale) {
        return prepare(operation.change(), locale);
    }

    Card apply(Operation operation, Locale locale) {
        var current = refresh(operation, locale);
        if (!Objects.equals(operation.snapshot(), current.snapshot())) throw new StaleOperation(current);
        var change = current.change();
        Long listId = change.listId();
        String code = change.productCode();
        switch (change.kind()) {
            case ADD_CART -> applyCartAdd(change, locale);
            case REMOVE_CART -> removeCartItem(change);
            case CREATE_LIST -> listId = shopping.createList(change.name());
            case RENAME_LIST -> shopping.renameList(listId, change.name());
            case DELETE_LIST -> {
                shopping.deleteList(listId);
                listId = null;
            }
            case ADD_WISHLIST -> tracking.save(code);
            case REMOVE_WISHLIST -> tracking.removeSaved(code);
            case CREATE_ALERT -> watches.save(code, null, change.alert().target(), change.alert().startAt(),
                    change.alert().repeat());
            case UPDATE_ALERT, TOGGLE_ALERT, DELETE_ALERT -> applyRuleChange(change, locale);
        }
        return new Card("receipt", ui(locale, "saved"), current.preview().detail(), current.preview().rows(), listId);
    }

    private Operation prepare(Change change, Locale locale) {
        return switch (change.kind()) {
            case ADD_CART, REMOVE_CART -> prepareItem(change, locale);
            case CREATE_LIST, RENAME_LIST, DELETE_LIST -> prepareList(change, locale);
            case ADD_WISHLIST, REMOVE_WISHLIST -> prepareWishlist(change, locale);
            case CREATE_ALERT -> {
                var product = activeProduct(change.productCode());
                requireAlertWishlist(change.productCode(), change.alert().target() != null);
                yield new Operation(change, "new", preview(change, locale,
                        description(locale, change.alert().target(), change.alert().startAt(), change.alert().repeat()),
                        List.of(new Row(label(product, locale), null, product.code()))));
            }
            case UPDATE_ALERT, TOGGLE_ALERT, DELETE_ALERT -> prepareRuleChange(change, locale);
        };
    }

    private Operation prepareItem(Change change, Locale locale) {
        var list = cartById(locale, change.listId());
        var existing = cartItem(list.id(), change.productCode());
        if (change.kind() == Kind.REMOVE_CART && existing == null) throw invalid("That item is no longer in the list.");
        ProductView product = change.kind() == Kind.ADD_CART ? activeProduct(change.productCode()) : existing.product();
        if (change.kind() == Kind.ADD_CART) {
            quantity((long) change.quantity() + (existing == null ? 0 : existing.quantity()), 1);
            validateRetailer(product, change.retailer(), existing == null ? null : existing.selectedRetailerCode());
            if (existing != null && change.retailer() != null
                    && !Objects.equals(normalizedRetailer(change.retailer()), existing.selectedRetailerCode())) {
                throw invalid("This item already has a different retailer choice. Change it in Cart before adding these packs.");
            }
        } else if (change.quantity() != null && change.quantity() > existing.quantity()) {
            throw invalid("There are fewer packs in the list than requested for removal.");
        }
        String retailer = change.retailer() != null ? normalizedRetailer(change.retailer())
                : existing == null ? null : existing.selectedRetailerCode();
        String count = ui(locale, "packs", change.quantity() == null ? existing.quantity() : change.quantity());
        if (retailer != null) count += " · " + retailer;
        var row = new Row(label(product, locale), count, product.code());
        return new Operation(change, new CartSnapshot(list.name(), savedItem(existing)),
                preview(change, locale, list.name(), List.of(row)));
    }

    private Operation prepareList(Change change, Locale locale) {
        boolean creating = change.kind() == Kind.CREATE_LIST;
        boolean deleting = change.kind() == Kind.DELETE_LIST;
        CartRef list = creating ? null : cartById(locale, change.listId());
        if (!creating) requireNamedList(list);
        if (!deleting) validateListName(change.name(), change.listId());
        var items = deleting ? shopping.items(list.id()).stream().map(AskShoppingTools::savedItem).toList() : List.<SavedItem>of();
        String detail = creating ? change.name() : deleting
                ? list.name() + " · " + ui(locale, "listItems", items.size()) : list.name() + " → " + change.name();
        return new Operation(change, creating ? "new" : new ListSnapshot(list.name(), items),
                preview(change, locale, detail, List.of()));
    }

    private Operation prepareWishlist(Change change, Locale locale) {
        var saved = tracking.savedProducts().stream()
                .filter(item -> item.product().code().equals(change.productCode())).findFirst().orElse(null);
        var product = change.kind() == Kind.ADD_WISHLIST ? activeProduct(change.productCode())
                : saved == null ? null : saved.product();
        if (product == null) throw invalid("That item is no longer in Wishlist.");
        return new Operation(change, saved == null ? null : saved.savedAt(), preview(change, locale, ui(locale, "wishlist"),
                List.of(new Row(label(product, locale), null, product.code()))));
    }

    private Operation prepareRuleChange(Change change, Locale locale) {
        var rule = resolveRule(change.ruleId(), locale);
        if (!Objects.equals(rule.productCode(), change.productCode())) throw invalid("That rule no longer matches the product.");
        String detail = rule.detail();
        if (change.kind() == Kind.TOGGLE_ALERT) {
            detail = ui(locale, Boolean.TRUE.equals(change.alert().enabled()) ? "enable" : "pause") + " · " + detail;
            if (Boolean.TRUE.equals(change.alert().enabled()) && rule.preferenceSnapshot() instanceof WatchRuleView saved
                    && saved.completed()) throw invalid("This one-time rule has completed. Edit the rule to schedule it again.");
        } else if (change.kind() == Kind.UPDATE_ALERT) {
            detail = updatedDescription(rule, change, locale);
        }
        return new Operation(change, new RuleSnapshot(rule.enabled(), rule.preferenceSnapshot()),
                preview(change, locale, detail, List.of(new Row(rule.label(), null, rule.productCode()))));
    }

    private String updatedDescription(RuleRow rule, Change change, Locale locale) {
        var settings = change.alert();
        var target = coalesce(settings.target(), rule.target());
        var date = coalesce(settings.startAt(), rule.startAt());
        var repeat = coalesce(settings.repeat(), rule.repeat());
        var interval = coalesce(settings.intervalDays(), rule.intervalDays());
        return switch (rule.type()) {
            case "reminder" -> {
                if (settings.target() != null || settings.startAt() != null || settings.repeat() != null) {
                    throw invalid("Use packs, interval days, or retailer for this existing interval-based reminder.");
                }
                int packs = coalesce(change.quantity(), rule.quantity());
                validateRetailer(product(rule.productCode()), change.retailer(), rule.retailer());
                String retailer = change.retailer() == null ? rule.retailer() : normalizedRetailer(change.retailer());
                yield ui(locale, "packs", packs) + " · " + ui(locale, "days", interval)
                        + " · " + (retailer == null ? ui(locale, "automatic") : retailer);
            }
            case "watch" -> {
                boolean legacy = rule.repeat() == null && settings.repeat() == null && settings.startAt() == null;
                if (legacy) {
                    if ((rule.intervalDays() == null) != (interval == null)) {
                        throw invalid("Create a separate rule to add or remove this interval.");
                    }
                    if (interval != null && interval > 365) throw invalid("Choose an interval from 1 to 365 days.");
                    yield describeTarget(locale, target) + (interval == null ? "" : " · " + ui(locale, "days", interval));
                }
                if (repeat == null) throw invalid("Specify whether this alert repeats.");
                if (rule.repeat() == null) throw invalid("Create a new calendar rule to replace this older interval rule.");
                if (target == null && date == null) throw invalid("Specify a price target or calendar time.");
                if (rule.target() == null && target != null) requireAlertWishlist(rule.productCode(), true);
                yield description(locale, target, date, repeat);
            }
            case "target", "retailer" -> {
                if (settings.target() == null || settings.startAt() != null || settings.repeat() != null
                        || settings.intervalDays() != null) throw invalid("Specify a new target price for this existing rule.");
                yield describeTarget(locale, target);
            }
            default -> throw invalid("Unsupported alert rule.");
        };
    }

    private void applyCartAdd(Change change, Locale locale) {
        String request = change.retailer();
        String retailer = request == null ? null : normalizedRetailer(request);
        shopping.add(change.listId(), change.productCode(), change.quantity(), request == null ? null : retailer == null ? "" : retailer);
    }

    private void removeCartItem(Change change) {
        var item = cartItem(change.listId(), change.productCode());
        if (item == null) throw invalid("That item is no longer in the list.");
        if (change.quantity() == null || change.quantity() == item.quantity()) {
            shopping.remove(change.listId(), change.productCode());
        } else {
            shopping.editDetails(change.listId(), change.productCode(), item.quantity() - change.quantity(),
                    item.selectedRetailerCode());
        }
    }

    private void applyRuleChange(Change change, Locale locale) {
        var rule = resolveRule(change.ruleId(), locale);
        switch (change.kind()) {
            case DELETE_ALERT -> {
                switch (rule.type()) {
                    case "watch" -> watches.delete(rule.productCode(), rule.id());
                    case "target" -> targets.deleteTarget(rule.productCode());
                    case "retailer" -> tracking.deleteLegacyAlerts(rule.productCode(), rule.id());
                    case "reminder" -> reminders.delete(rule.id());
                    default -> throw invalid("Unsupported alert rule.");
                }
            }
            case TOGGLE_ALERT -> {
                boolean enabled = change.alert().enabled();
                switch (rule.type()) {
                    case "watch" -> watches.setEnabled(rule.productCode(), rule.id(), enabled);
                    case "target" -> targets.setEnabled(rule.productCode(), enabled);
                    case "retailer" -> tracking.setLegacyAlertsEnabled(rule.productCode(), rule.id(), enabled);
                    case "reminder" -> reminders.setEnabled(rule.id(), enabled);
                    default -> throw invalid("Unsupported alert rule.");
                }
            }
            case UPDATE_ALERT -> {
                var input = change.alert();
                var target = coalesce(input.target(), rule.target());
                var date = coalesce(input.startAt(), rule.startAt());
                var repeat = coalesce(input.repeat(), rule.repeat());
                var interval = coalesce(input.intervalDays(), rule.intervalDays());
                switch (rule.type()) {
                    case "watch" -> {
                        if (repeat == null) watches.save(rule.productCode(), rule.id(), target, interval);
                        else watches.save(rule.productCode(), rule.id(), target, date, repeat);
                    }
                    case "target" -> targets.setTarget(rule.productCode(), target);
                    case "retailer" -> tracking.configureTarget(rule.id(), target);
                    case "reminder" -> reminders.update(rule.id(), coalesce(change.quantity(), rule.quantity()), interval,
                            change.retailer() == null ? rule.retailer() : normalizedRetailer(change.retailer()));
                    default -> throw invalid("Unsupported alert rule.");
                }
            }
            default -> throw invalid("Unsupported alert change.");
        }
    }

    private List<RuleRow> configuredRules(Locale locale) {
        var rows = new ArrayList<RuleRow>();
        for (var rule : watches.allRules()) {
            String detail = rule.repeat() == null ? describeTarget(locale, rule.target())
                    + (rule.intervalDays() == null ? "" : " · " + ui(locale, "days", rule.intervalDays()))
                    : description(locale, rule.target(), rule.startAt(), rule.repeat());
            if (rule.nextDueAt() != null) detail += " · " + ui(locale, "due", localTime(rule.nextDueAt()));
            if (rule.waitingForLive()) detail += " · " + ui(locale, "waiting");
            if (rule.completed()) detail += " · " + ui(locale, "completed");
            rows.add(new RuleRow("watch:" + rule.id(), rule.productCode(), label(rule.productCode(), locale),
                    status(locale, detail, rule.enabled()), "watch", rule.id(), rule.target(), rule.startAt(),
                    rule.repeat(), rule.intervalDays(), null, null, rule.enabled(), rule));
        }
        for (var rule : targets.rules()) {
            rows.add(new RuleRow("target:" + rule.productCode(), rule.productCode(), label(rule.productCode(), locale),
                    status(locale, describeTarget(locale, rule.target()), rule.enabled()), "target", null, rule.target(),
                    null, null, null, null, null, rule.enabled(), rule));
        }
        for (var rule : tracking.legacyRules()) {
            String detail = rule.retailerCode();
            if (rule.targetPrice() != null) detail += " · " + describeTarget(locale, rule.targetPrice());
            if (rule.dropEnabled()) detail += " · " + ui(locale, "drop");
            rows.add(new RuleRow("retailer:" + rule.id(), rule.productCode(), label(rule.productCode(), locale),
                    status(locale, detail, rule.alertsEnabled()), "retailer", rule.id(), rule.targetPrice(), null,
                    null, null, null, rule.retailerCode(), rule.alertsEnabled(), rule));
        }
        for (var rule : reminders.overview().reminders()) {
            String detail = ui(locale, "packs", rule.quantity()) + " · " + ui(locale, "days", rule.intervalDays())
                    + " · " + ui(locale, "due", localTime(rule.effectiveDueAt()));
            if (rule.selectedRetailerCode() != null) detail += " · " + rule.selectedRetailerCode();
            rows.add(new RuleRow("reminder:" + rule.id(), rule.productCode(), label(rule.productCode(), locale),
                    status(locale, detail, rule.enabled()), "reminder", rule.id(), null, null, null, rule.intervalDays(),
                    rule.quantity(), rule.selectedRetailerCode(), rule.enabled(), rule));
        }
        return List.copyOf(rows);
    }

    private List<EventRow> alertHistory(Locale locale, String requestedCode) {
        var events = new ArrayList<EventRow>();
        var now = clock.instant();
        for (var event : alerts.events(now)) {
            if (event.productCode() == null) continue;
            String detail = event.price() == null ? ui(locale, "unknown") : money(event.price());
            if (event.retailerCode() != null) detail += " · " + event.retailerCode();
            events.add(event(locale, event.productCode(), detail, event.createdAt()));
        }
        for (var event : targets.events(now)) {
            String detail = event.target() == null ? ui(locale, "timeReminder") : describeTarget(locale, event.target());
            if (!event.retailers().isEmpty()) {
                var observed = event.retailers().stream().map(price -> price.retailerCode() + " " + money(price.price()))
                        .toList();
                detail += " · " + String.join(", ", observed);
            }
            if (event.retrievedAt() != null) detail += " · " + ui(locale, "retrieved", localTime(event.retrievedAt()));
            events.add(event(locale, event.productCode(), detail, event.createdAt()));
        }
        for (var event : reminders.occurrences(now)) {
            events.add(event(locale, event.productCode(), ui(locale, "timeReminder")
                    + " · " + ui(locale, "packs", event.quantity()), event.createdAt()));
        }
        return events.stream().filter(event -> requestedCode == null || requestedCode.equals(event.productCode()))
                .sorted(Comparator.comparing(EventRow::createdAt).reversed()).limit(MAX_ROWS).toList();
    }

    private EventRow event(Locale locale, String code, String detail, Instant createdAt) {
        return new EventRow(code, label(code, locale), localTime(createdAt) + " · " + detail, createdAt);
    }

    private RuleRow resolveRule(String ruleId, Locale locale) {
        return configuredRules(locale).stream().filter(rule -> rule.ruleId().equals(ruleId)).findFirst()
                .orElseThrow(() -> invalid("That rule could not be found. Read the saved alert rules first."));
    }

    private static Map<String, Object> ruleDetails(RuleRow rule) {
        var details = new LinkedHashMap<String, Object>();
        details.put("rule_id", rule.ruleId());
        details.put("product_code", rule.productCode());
        details.put("label", rule.label());
        details.put("detail", rule.detail());
        details.put("type", rule.type());
        details.put("enabled", rule.enabled());
        details.put("target", rule.target());
        details.put("start_at", stringDate(rule.startAt()));
        details.put("repeat", rule.repeat());
        details.put("interval_days", rule.intervalDays());
        details.put("quantity", rule.quantity());
        details.put("retailer", rule.retailer());
        return details;
    }

    private Card preview(Change change, Locale locale, String detail, List<Row> rows) {
        String action = switch (change.kind()) {
            case ADD_CART -> "addCart";
            case REMOVE_CART -> "removeCart";
            case CREATE_LIST -> "createList";
            case RENAME_LIST -> "renameList";
            case DELETE_LIST -> "deleteList";
            case ADD_WISHLIST -> "addWishlist";
            case REMOVE_WISHLIST -> "removeWishlist";
            case CREATE_ALERT -> "createAlert";
            case UPDATE_ALERT -> "updateAlert";
            case TOGGLE_ALERT -> "toggleAlert";
            case DELETE_ALERT -> "deleteAlert";
        };
        String summary = ui(locale, action) + " · " + detail;
        return new Card("confirmation", ui(locale, change.kind().confirm ? "confirm" : "prepared"),
                summary, rows, change.listId());
    }

    private List<CartRef> carts(Locale locale) {
        var saved = shopping.lists();
        var result = new ArrayList<CartRef>();
        result.add(new CartRef(null, ui(locale, "cart")));
        saved.stream().filter(item -> !"My list".equals(item.name()))
                .map(item -> new CartRef(item.id(), item.name())).forEach(result::add);
        return List.copyOf(result);
    }

    private CartRef cartById(Locale locale, Long id) {
        return carts(locale).stream().filter(list -> Objects.equals(list.id(), id)).findFirst()
                .orElseThrow(() -> invalid("That saved list no longer exists."));
    }

    private static Long requireNamedList(CartRef list) {
        if (list.id() == null) throw invalid("The main Cart cannot be renamed or deleted.");
        return list.id();
    }

    private void validateListName(String name, Long currentId) {
        if (name.length() > 100 || name.chars().anyMatch(Character::isISOControl)
                || "Cart".equalsIgnoreCase(name) || "My list".equalsIgnoreCase(name)) {
            throw invalid("Enter a list name from 1 to 100 characters that is not reserved.");
        }
        if (shopping.lists().stream().anyMatch(list -> !Objects.equals(currentId, list.id())
                && list.name().equalsIgnoreCase(name))) throw invalid("A shopping list already has that name.");
    }

    private ItemView cartItem(Long listId, String code) {
        return shopping.items(listId).stream().filter(item -> item.product().code().equals(code)).findFirst().orElse(null);
    }

    private static SavedItem savedItem(ItemView item) {
        return item == null ? null : new SavedItem(item.product().code(), item.quantity(), item.selectedRetailerCode(), item.checked());
    }

    private ProductView activeProduct(String code) {
        var product = product(code);
        if (!product.active()) throw invalid("That exact pack is not active in the catalogue.");
        return product;
    }

    private ProductView product(String code) {
        if (code == null) throw invalid("Choose an exact product pack first.");
        return catalogue.find(code);
    }

    private void requireAlertWishlist(String code, boolean required) {
        if (required && tracking.savedProducts().stream().noneMatch(item -> item.product().code().equals(code))) {
            throw invalid("Save this pack to Wishlist before preparing a price alert. Ask the shopper if they want to add it.");
        }
    }

    private static void validateRetailer(ProductView product, String requested, String existing) {
        if (requested == null) return;
        String code = normalizedRetailer(requested);
        if (code == null || code.equals(existing)) return;
        if (product.listings().stream().noneMatch(listing -> code.equals(listing.retailerCode()) && listing.price() != null)) {
            throw invalid("Choose automatic or a retailer code with a current saved price.");
        }
    }

    private static String normalizedRetailer(String retailer) {
        return retailer == null || "automatic".equalsIgnoreCase(retailer) ? null : retailer;
    }

    private String label(String code, Locale locale) {
        try {
            return label(product(code), locale);
        } catch (IllegalArgumentException ignored) {
            return code;
        }
    }

    private static String label(ProductView product, Locale locale) {
        String brand = product.brand().forLocale(locale);
        String name = product.name().forLocale(locale);
        return brand.isBlank() ? name : brand + " · " + name;
    }

    private String description(Locale locale, BigDecimal target, LocalDateTime start, String repeat) {
        var parts = new ArrayList<String>();
        if (target != null) parts.add(describeTarget(locale, target));
        if (start != null) parts.add(ui(locale, "starts", printableDate(start)));
        if (repeat != null) parts.add(messages.getMessage("watchEditor.repeat." + repeat, null, repeat, locale));
        return String.join(" · ", parts);
    }

    private String describeTarget(Locale locale, BigDecimal price) {
        return ui(locale, "target", money(price));
    }

    private String status(Locale locale, String detail, boolean enabled) {
        return detail + " · " + ui(locale, enabled ? "on" : "off");
    }

    private String ui(Locale locale, String key, Object... args) {
        return messages.getMessage("askShopping." + key, args, key, locale);
    }

    private static String money(BigDecimal amount) {
        var format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-HK"));
        format.setCurrency(Currency.getInstance("HKD"));
        format.setMaximumFractionDigits(Math.max(2, amount.stripTrailingZeros().scale()));
        return format.format(amount);
    }

    private static String localTime(Instant date) {
        return date == null ? "" : printableDate(LocalDateTime.ofInstant(date, HONG_KONG));
    }

    private static String printableDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'HKT'"));
    }

    private static String stringDate(LocalDateTime date) {
        return date == null ? null : date.toString();
    }

    private static <T> T coalesce(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private static int quantity(Long value, int fallback) {
        if (value == null) return fallback;
        if (value < 1 || value > 999) throw invalid("Choose a whole number of packs between 1 and 999.");
        return value.intValue();
    }

    private static Integer optionalQuantity(Long value) {
        return value == null ? null : quantity(value, 1);
    }

    private static Integer optionalInterval(Long value) {
        if (value == null) return null;
        if (value < 1 || value > Integer.MAX_VALUE) throw invalid("Choose a valid number of days.");
        return value.intValue();
    }

    private static BigDecimal optionalPrice(JsonNode args) {
        JsonNode field = args.get("target");
        if (field == null || field.isNull()) return null;
        String value = field.isNumber() ? field.asText() : arg(args, "target", 40);
        if (value == null) return null;
        if (!value.matches("[0-9]{1,15}(\\.[0-9]{1,12})?")) throw invalid("Use a valid target amount in HKD.");
        try {
            return new BigDecimal(value).setScale(4, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ignored) {
            throw invalid("Use at most four decimal places for the target.");
        }
    }

    private static LocalDateTime optionalDate(JsonNode args) {
        String value = arg(args, "start_at", 30);
        if (value == null) return null;
        try {
            var date = LocalDateTime.parse(value);
            if (date.getYear() < 1 || date.getYear() > 9999) throw invalid("Use a valid Hong Kong calendar date.");
            return date;
        } catch (DateTimeParseException ignored) {
            throw invalid("Use a Hong Kong date and time like YYYY-MM-DDTHH:mm.");
        }
    }

    private static String optionalRepeat(JsonNode args) {
        String value = arg(args, "repeat", 20);
        if (value == null) return null;
        try {
            return WatchRepeat.valueOf(value).name();
        } catch (IllegalArgumentException ignored) {
            throw invalid("Choose one-time, daily, weekly, fortnightly, or monthly.");
        }
    }

    private static String required(JsonNode args, String key, int maxLength) {
        String value = arg(args, key, maxLength);
        if (value == null) throw invalid("Provide " + key + ".");
        return value;
    }

    private static String arg(JsonNode args, String key, int maxLength) {
        JsonNode field = args.get(key);
        if (field == null || field.isNull()) return null;
        if (!field.isTextual() || field.textValue().length() > maxLength) throw invalid("Use valid text for " + key + ".");
        String value = field.textValue().strip();
        return value.isEmpty() ? null : value;
    }

    private static Long number(JsonNode args, String key) {
        JsonNode field = args.get(key);
        if (field == null || field.isNull()) return null;
        if (!field.isIntegralNumber() || !field.canConvertToLong()) throw invalid("Use a whole number for " + key + ".");
        return field.longValue();
    }

    private static Boolean flag(JsonNode args, String key) {
        JsonNode field = args.get(key);
        if (field == null || field.isNull()) return null;
        if (!field.isBoolean()) throw invalid("Use true or false for " + key + ".");
        return field.booleanValue();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static DeepSeekChatClient.Tool tool(String name, String description, Map<String, Object> properties,
                                               String... required) {
        return new DeepSeekChatClient.Tool(name, description, Map.of("type", "object", "properties", properties,
                "required", List.of(required), "additionalProperties", false));
    }

    private static Map<String, Object> choice(String... values) {
        return Map.of("type", "string", "enum", List.of(values));
    }

    private static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static Map<String, Object> integer(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private static Map<String, Object> bool(String description) {
        return Map.of("type", "boolean", "description", description);
    }

    private static Map<String, Object> fields(Object... fields) {
        var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < fields.length; i += 2) result.put((String) fields[i], fields[i + 1]);
        return result;
    }

    public record Draft(String productCode, String condition, BigDecimal target, LocalDateTime startAt, String repeat) {}

    public record Card(String kind, String title, String detail, List<Row> rows, Long listId) {
        public Card { rows = List.copyOf(rows); }
    }

    public record Row(String label, String detail, String productCode, Long listId) {
        public Row(String label, String detail, String productCode) { this(label, detail, productCode, null); }
    }

    public record Operation(Change change, Object snapshot, Card preview) {
        public boolean requiresConfirmation() { return change.kind().confirm; }
        String conflictKey() { return change.key(); }
    }

    static final class StaleOperation extends RuntimeException {
        private final Operation operation;

        StaleOperation(Operation operation) { this.operation = operation; }
        Operation operation() { return operation; }
    }

    enum Kind {
        ADD_CART(false), REMOVE_CART(true), CREATE_LIST(false), RENAME_LIST(false), DELETE_LIST(true),
        ADD_WISHLIST(false), REMOVE_WISHLIST(true), CREATE_ALERT(true), UPDATE_ALERT(true), TOGGLE_ALERT(true),
        DELETE_ALERT(true);

        private final boolean confirm;
        Kind(boolean confirm) { this.confirm = confirm; }
    }

    record Change(Kind kind, Long listId, String productCode, Integer quantity, String retailer, String name,
                  String ruleId, AlertChange alert) {
        String key() {
            return switch (kind) {
                case CREATE_LIST -> "new:" + name.toLowerCase(Locale.ROOT);
                case RENAME_LIST, DELETE_LIST -> "list:" + listId;
                case ADD_CART, REMOVE_CART -> "cart:" + listId + ":" + productCode;
                case ADD_WISHLIST, REMOVE_WISHLIST -> "wishlist:" + productCode;
                case CREATE_ALERT -> "new-alert:" + productCode;
                case UPDATE_ALERT, TOGGLE_ALERT, DELETE_ALERT -> "alert:" + ruleId;
            };
        }
    }

    private record AlertChange(BigDecimal target, LocalDateTime startAt, String repeat, Integer intervalDays, Boolean enabled) {}
    private record CartRef(Long id, String name) {}
    private record CartRow(String productCode, String label, int quantity, String retailer, String selectedRetailer,
                           BigDecimal unitPrice, BigDecimal total, boolean checked) {}
    private record CartReply(CartRef list, List<CartRow> items, BigDecimal knownSubtotal,
                             int unknownItemCount, String budgetPeriod, BigDecimal budgetLimit, int totalItems) {}
    private record SavedRow(String productCode, String label, BigDecimal lowestPrice) {}
    private record EventRow(String productCode, String label, String detail, Instant createdAt) {}
    private record RuleRow(String ruleId, String productCode, String label, String detail, String type, Long id,
                           BigDecimal target, LocalDateTime startAt, String repeat, Integer intervalDays,
                           Integer quantity, String retailer, boolean enabled, Object preferenceSnapshot) {}
    private record SavedItem(String productCode, int quantity, String retailer, boolean checked) {}
    private record ListSnapshot(String name, List<SavedItem> items) {}
    private record CartSnapshot(String name, SavedItem item) {}
    private record RuleSnapshot(boolean enabled, Object settings) {}
}
