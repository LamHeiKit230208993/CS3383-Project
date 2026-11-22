package com.pricewatch.web;

import com.pricewatch.budget.BudgetService;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.shopping.ListTransfer;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import com.pricewatch.tracking.DemoHistoryService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;

@Controller
public class HelloController {
    private static final Map<String, String> PLANNED = Map.of(
            "saved", "Wishlist and tracking", "updates", "Alerts and reminders", "ask", "Price assistant");
    private final CatalogService catalog;
    private final ShoppingService shopping;
    private final BudgetService budgets;
    private final MessageSource messages;
    private final DemoHistoryService demoHistory;

    @org.springframework.beans.factory.annotation.Autowired
    public HelloController(CatalogService catalog, ShoppingService shopping, BudgetService budgets,
                           MessageSource messages, DemoHistoryService demoHistory) {
        this.catalog = catalog;
        this.shopping = shopping;
        this.budgets = budgets;
        this.messages = messages;
        this.demoHistory = demoHistory;
    }

    public HelloController(CatalogService catalog, ShoppingService shopping, BudgetService budgets,
                           MessageSource messages) {
        this(catalog, shopping, budgets, messages, null);
    }
    @ModelAttribute
    void common(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", this);
    }
    @GetMapping("/")
    String discover(@RequestParam(defaultValue = "") String q,
                    @RequestParam(defaultValue = "1") int page,
                    @RequestParam(defaultValue = "") String category,
                    @RequestParam(defaultValue = "false") boolean related, Model model) {
        related = related && !q.isBlank();
        var cartItems = shopping.items();
        addEstimate(model, Summary.from(cartItems));
        model.addAttribute("cartLineCount", cartItems.size());
        model.addAttribute("previewItems", cartItems.subList(0, Math.min(cartItems.size(), 3)));
        var search = catalog.searchResults(q, category);
        var results = related ? search.related() : search.matches();
        model.addAttribute("category", category);
        model.addAttribute("categories", search.categories());
        model.addAttribute("categoryLabels", search.categoryLabels());
        model.addAttribute("related", related);
        model.addAttribute("relatedCount", search.related().size());
        model.addAttribute("directCount", search.matches().size());
        int pages = Math.max(1, (results.size() + 23) / 24);
        int current = Math.max(1, Math.min(page, pages));
        model.addAttribute("q", q);
        model.addAttribute("products", results.subList((current - 1) * 24, Math.min(current * 24, results.size())));
        model.addAttribute("resultCount", results.size());
        model.addAttribute("pageNumber", current);
        model.addAttribute("pageCount", pages);
        return screen(model, "discover", "Discover");
    }
    @GetMapping("/products/{code}")
    String compare(@PathVariable String code, @RequestParam(defaultValue = "false") boolean fromCart,
                   @RequestParam(defaultValue = "false") boolean fromWishlist,
                   @RequestParam(required = false) Long listId, Model model) {
        try { model.addAttribute("product", catalog.find(code)); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"); }
        model.addAttribute("fromCart", fromCart);
        model.addAttribute("fromWishlist", !fromCart && fromWishlist);
        model.addAttribute("returnListId", listId);
        model.addAttribute("cartChoices", shopping.lists());
        return screen(model, "compare", "Compare listed prices");
    }
    @GetMapping("/list")
    String list(@RequestParam(required = false) Long listId, Model model) {
        var lists = shopping.lists();
        var selected = listId == null ? lists.getFirst() : lists.stream()
                .filter(choice -> listId.equals(choice.id())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var items = listId == null ? shopping.items() : shopping.items(listId);
        model.addAttribute("lists", lists);
        model.addAttribute("selectedList", selected);
        model.addAttribute("namedList", selected.id() != null && !"My list".equals(selected.name()));
        model.addAttribute("listId", listId);
        model.addAttribute("items", items);
        addEstimate(model, Summary.from(items));
        return screen(model, "list", "Cart");
    }

    @PostMapping("/list/new")
    String createCart(@RequestParam String name, RedirectAttributes redirect) {
        try {
            long listId = shopping.createList(name);
            redirect.addFlashAttribute("notice", text("cart.listCreated"));
            return listRedirect(listId);
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", shoppingError(exception));
            return listRedirect(null);
        }
    }

    @PostMapping("/list/rename")
    String renameCart(@RequestParam long listId, @RequestParam String name, RedirectAttributes redirect) {
        try {
            shopping.renameList(listId, name);
            redirect.addFlashAttribute("notice", text("cart.listRenamed"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", shoppingError(exception));
            return existingListRedirect(listId);
        }
        return listRedirect(listId);
    }

    @PostMapping("/list/delete")
    String deleteCart(@RequestParam long listId, @RequestParam(defaultValue = "false") boolean confirmed,
                      RedirectAttributes redirect) {
        if (!confirmed) {
            redirect.addFlashAttribute("error", text("cart.confirmDeleteList"));
            return existingListRedirect(listId);
        }
        try {
            shopping.deleteList(listId);
            redirect.addFlashAttribute("notice", text("cart.listDeleted"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", shoppingError(exception));
            return existingListRedirect(listId);
        }
        return listRedirect(null);
    }

    private void addEstimate(Model model, Summary summary) {
        model.addAttribute("summary", summary);
        model.addAttribute("budget", budgets.forSummary(summary));
    }
    @GetMapping(value = "/list/export", produces = "application/json")
    ResponseEntity<ListTransfer> exportList(
            @RequestParam(required = false) Long listId) {
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=shopping-list.json")
                    .body(shopping.exportList(listId));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/list/import")
    String importList(@RequestParam("file") MultipartFile file,
                      RedirectAttributes redirect) {
        try (var input = file.getInputStream()) {
            long id = shopping.importList(input);
            redirect.addFlashAttribute("notice", text("feedback.list.imported"));
            return "redirect:/list?listId=" + id;
        } catch (IOException | IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", text("feedback.list.importFailed"));
            return "redirect:/list";
        }
    }
    @GetMapping("/data")
    String data(Model model) { return screen(model, "data", "Price data & updates"); }
    @GetMapping("/planned/{feature}")
    String planned(@PathVariable String feature, Model model) {
        if (feature.equals("saved")) return "redirect:/saved";
        if (feature.equals("updates")) return "redirect:/alerts";
        if (!PLANNED.containsKey(feature)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("feature", feature);
        return screen(model, "planned", PLANNED.get(feature));
    }
    @PostMapping("/data/{action:sample|refresh|demo}")
    String load(@PathVariable String action, @RequestParam(defaultValue = "false") boolean confirmSample,
                @RequestParam(defaultValue = "false") boolean fromSettings, RedirectAttributes redirect) {
        String next = fromSettings ? "redirect:/settings#price-data" : "redirect:/data";
        String error = fromSettings ? "dataError" : "error";
        if ((action.equals("sample") || action.equals("demo")) && catalog.status() != null && !confirmSample) {
            redirect.addFlashAttribute(error, text("data.confirmation.required"));
            return next;
        }
        try {
            if (action.equals("demo")) {
                if (demoHistory == null) throw new IllegalStateException("Demo history is unavailable.");
                var demo = demoHistory.seed();
                redirect.addFlashAttribute(fromSettings ? "dataNotice" : "notice",
                        text("data.demo.accepted", Integer.toString(demo.productCount()),
                                Integer.toString(demo.observationCount())));
                return "redirect:/saved";
            }
            var accepted = action.equals("sample") ? catalog.importSample() : catalog.refreshLive();
            redirect.addFlashAttribute(fromSettings ? "dataNotice" : "notice", text(action.equals("sample") ? "data.sample.accepted" : "data.live.accepted",
                    Integer.toString(accepted.productCount())));
        } catch (RuntimeException exception) {
            // Keep provider and database details out of user-facing HTML.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Catalogue import failed", exception);
            redirect.addFlashAttribute(error, text("data.import.failed"));
        }
        return next;
    }
    @PostMapping("/list/items")
    String add(@RequestParam String code, @RequestParam int quantity,
               @RequestParam(defaultValue = "") String retailerCode, @RequestParam(required = false) Long listId,
               RedirectAttributes redirect) {
        try {
            shopping.add(listId, code, quantity, retailerCode);
            redirect.addFlashAttribute("notice", text("feedback.list.added"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", shoppingError(exception));
            return existingListRedirect(listId);
        }
        return listRedirect(listId);
    }
    @PostMapping({"/list/items/{pathCode}/update", "/list/items/update"})
    String update(@PathVariable(required = false) String pathCode,
                  @RequestParam(required = false) String code, @RequestParam int quantity,
                  @RequestParam(defaultValue = "false") boolean checked,
                  @RequestParam(required = false) String retailerCode,
                  @RequestParam(required = false) Long listId, RedirectAttributes redirect) {
        try {
            code = itemCode(pathCode, code);
            if (listId != null) shopping.update(listId, code, quantity, checked, retailerCode);
            else if (retailerCode == null) shopping.update(code, quantity, checked);
            else shopping.update(code, quantity, checked, retailerCode);
            redirect.addFlashAttribute("notice", text("feedback.list.saved"));
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", shoppingError(exception)); }
        return listRedirect(listId);
    }

    @PostMapping("/list/items/details")
    String editCartItem(@RequestParam String code, @RequestParam int quantity,
                        @RequestParam(defaultValue = "") String retailerCode,
                        @RequestParam(required = false) Long listId, RedirectAttributes redirect) {
        try {
            shopping.editDetails(listId, code, quantity, retailerCode);
            redirect.addFlashAttribute("notice", text("feedback.list.saved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", shoppingError(exception));
        }
        return listRedirect(listId);
    }

    @PostMapping(value = "/list/items/check", params = "!inline")
    String checkCartItem(@RequestParam String code, @RequestParam(defaultValue = "false") boolean checked,
                         @RequestParam(required = false) Long listId) {
        checkCartItemInline(code, checked, listId);
        return listRedirect(listId);
    }

    @PostMapping(value = "/list/items/check", params = "inline=true")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void checkCartItemInline(@RequestParam String code, @RequestParam(defaultValue = "false") boolean checked,
                             @RequestParam(required = false) Long listId) {
        try {
            shopping.checkOff(listId, code, checked);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
    @PostMapping({"/list/items/{pathCode}/remove", "/list/items/remove"})
    String remove(@PathVariable(required = false) String pathCode, @RequestParam(required = false) String code,
                  @RequestParam(required = false) Long listId, RedirectAttributes redirect) {
        try {
            code = itemCode(pathCode, code);
            if (listId == null) shopping.remove(code);
            else shopping.remove(listId, code);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirect.addFlashAttribute("notice", text("feedback.list.removed"));
        return listRedirect(listId);
    }
    private String itemCode(String pathCode, String code) {
        if (pathCode != null) return pathCode;
        if (code == null || code.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return code;
    }
    private String listRedirect(Long listId) {
        return listId == null ? "redirect:/list" : "redirect:/list?listId=" + listId;
    }

    private String existingListRedirect(Long listId) {
        return listId != null && shopping.lists().stream().anyMatch(choice -> listId.equals(choice.id()))
                ? listRedirect(listId) : listRedirect(null);
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    String oversizedUpload(Model model) {
        model.addAttribute("error", text("feedback.list.tooLarge"));
        return screen(model, "error", text("feedback.list.importErrorTitle"));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, org.springframework.web.bind.MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String badInput(Model model) {
        model.addAttribute("error", text("feedback.input"));
        return screen(model, "error", text("error.input.title"));
    }
    private String screen(Model model, String scene, String title) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", title);
        return "market-desk";
    }
    public boolean hasListingPrice(ProductView product, String retailerCode) {
        return product.listings().stream().anyMatch(listing ->
                listing.retailerCode().equals(retailerCode) && listing.price() != null);
    }

    public String money(BigDecimal value) {
        if (value == null) return "Not listed";
        var format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-HK"));
        format.setCurrency(Currency.getInstance("HKD"));
        format.setMaximumFractionDigits(Math.max(2, value.stripTrailingZeros().scale()));
        return format.format(value);
    }
    public String time(Instant value) {
        var locale = LocaleContextHolder.getLocale();
        String pattern = "zh".equals(locale.getLanguage()) ? "uuuu/MM/dd HH:mm 'HKT'" : "d MMM uuuu, HH:mm 'HKT'";
        return DateTimeFormatter.ofPattern(pattern, locale)
                .withZone(ZoneId.of("Asia/Hong_Kong")).format(value);
    }

    private String text(String key, Object... arguments) {
        return messages.getMessage(key, arguments, key, LocaleContextHolder.getLocale());
    }

    private String shoppingError(IllegalArgumentException exception) {
        return text(switch (exception.getMessage()) {
            case "That shopping list does not exist." -> "feedback.list.missing";
            case "The main Cart cannot be renamed or deleted." -> "cart.mainProtected";
            case "Enter a list name from 1 to 100 characters that is not reserved." -> "cart.invalidListName";
            case "A shopping list already has that name." -> "cart.duplicateListName";
            case "That product is not in the accepted catalogue." -> "feedback.list.productMissing";
            case "That product is no longer in the current catalogue." -> "feedback.list.productRetired";
            case "This product is already in your list with another retailer; change retailer in My list." ->
                    "feedback.list.retailerConflict";
            case "That item is not on your list." -> "feedback.list.itemMissing";
            case "Choose a retailer with a current price for this product." -> "feedback.list.invalidRetailer";
            case "Quantity must be a whole number from 1 to 999." -> "feedback.list.invalidQuantity";
            default -> "feedback.input";
        });
    }
}
