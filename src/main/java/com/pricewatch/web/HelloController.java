package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.chat.ChatReply;
import com.pricewatch.chat.ChatService;
import com.pricewatch.shopping.ListTransferService;
import com.pricewatch.shopping.OfflineListExporter;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
            "saved", "ui.title.plannedSaved", "updates", "ui.title.plannedUpdates");
    private final CatalogService catalog;
    private final ShoppingService shopping;
    private final ChatService chat;
    private final ListTransferService transfer;
    private final OfflineListExporter offlineExporter;
    private final UiMessages messages;
    private final com.pricewatch.tracking.DemoHistoryService demoHistory;

    public HelloController(CatalogService catalog, ShoppingService shopping, ChatService chat,
                           ListTransferService transfer, OfflineListExporter offlineExporter,
                           UiMessages messages,
                           com.pricewatch.tracking.DemoHistoryService demoHistory) {
        this.catalog = catalog;
        this.shopping = shopping;
        this.chat = chat;
        this.transfer = transfer;
        this.offlineExporter = offlineExporter;
        this.messages = messages;
        this.demoHistory = demoHistory;
    }
    @ModelAttribute
    public void common(Model model, jakarta.servlet.http.HttpServletRequest request) {
        // J12: templates translate through #{...}, which reads this holder.
        org.springframework.context.i18n.LocaleContextHolder.setLocale(LocaleController.requestLocale(request));
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", this);
    }
    @GetMapping("/")
    String discover(@RequestParam(defaultValue = "") String q,
                    @RequestParam(defaultValue = "1") int page,
                    @RequestParam(defaultValue = "") String category,
                    @RequestParam(defaultValue = "false") boolean related, Model model,
                    jakarta.servlet.http.HttpServletRequest request) {
        related = related && !q.isBlank();
        model.addAttribute("summary", shopping.summary());
        var search = catalog.searchResults(q, category);
        var results = related ? search.related() : search.matches();
        model.addAttribute("category", category);
        model.addAttribute("categories", search.categories());
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
        return screen(model, request, "discover", "ui.title.discover");
    }
    @GetMapping("/products/{code}")
    String compare(@PathVariable String code, Model model,
                   jakarta.servlet.http.HttpServletRequest request) {
        try { model.addAttribute("product", catalog.find(code)); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"); }
        return screen(model, request, "compare", "ui.title.compare");
    }
    @GetMapping("/list")
    String list(Model model, jakarta.servlet.http.HttpServletRequest request) {
        var items = shopping.items();
        model.addAttribute("items", items);
        model.addAttribute("summary", Summary.from(items));
        return screen(model, request, "list", "ui.title.list");
    }
    @GetMapping("/list/export")
    org.springframework.http.ResponseEntity<byte[]> exportList() throws java.io.IOException {
        byte[] body = transfer.export();
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"market-desk-list.json\"")
                .body(body);
    }
    @GetMapping("/list/offline")
    org.springframework.http.ResponseEntity<byte[]> exportOfflineList() {
        byte[] body = offlineExporter.export();
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("text/html;charset=UTF-8"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"market-desk-list.html\"")
                .body(body);
    }
    @GetMapping("/list/import")
    String importList(Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("preview", null);
        return screen(model, request, "import", "ui.title.import");
    }
    @PostMapping("/list/import")
    String importList(@RequestParam(defaultValue = "false") boolean confirm,
                      @RequestParam(required = false) org.springframework.web.multipart.MultipartFile file,
                      jakarta.servlet.http.HttpSession session, Model model, RedirectAttributes redirect,
                      jakarta.servlet.http.HttpServletRequest request) {
        String pendingKey = "pendingListImport";
        if (!confirm) {
            if (file == null || file.isEmpty()) {
                model.addAttribute("error", messages.get(request, "ui.error.chooseFile"));
                model.addAttribute("preview", null);
                return screen(model, request, "import", "ui.title.import");
            }
            try {
                byte[] bytes = file.getBytes();
                var preview = transfer.preview(bytes);
                session.setAttribute(pendingKey, bytes);
                model.addAttribute("preview", preview);
                return screen(model, request, "import", "ui.title.import");
            } catch (IllegalArgumentException | java.io.IOException exception) {
                session.removeAttribute(pendingKey);
                model.addAttribute("error", exception instanceof IllegalArgumentException runtime
                        ? messages.error(request, runtime) : exception.getMessage());
                model.addAttribute("preview", null);
                return screen(model, request, "import", "ui.title.import");
            }
        }
        Object pending = session.getAttribute(pendingKey);
        if (!(pending instanceof byte[] bytes)) {
            model.addAttribute("error", messages.get(request, "ui.error.reviewFileFirst"));
            model.addAttribute("preview", null);
            return screen(model, request, "import", "ui.title.import");
        }
        try {
            var result = transfer.importFile(bytes);
            session.removeAttribute(pendingKey);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.imported", result.itemCount()));
            return "redirect:/list";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", messages.error(request, exception));
            model.addAttribute("preview", null);
            return screen(model, request, "import", "ui.title.import");
        }
    }
    @GetMapping("/data")
    String data(Model model, jakarta.servlet.http.HttpServletRequest request) {
        // The page shows the accepted catalogue, so it needs the import status.
        model.addAttribute("importStatus", catalog.status());
        return screen(model, request, "data", "ui.title.data");
    }
    @GetMapping("/ask")
    String ask(Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("question", "");
        model.addAttribute("reply", null);
        return screen(model, request, "ask", "ui.title.ask");
    }
    @PostMapping("/ask")
    String ask(@RequestParam(defaultValue = "") String question, Model model,
               jakarta.servlet.http.HttpServletRequest request) {
        ChatReply reply = chat.reply(question);
        model.addAttribute("question", question);
        model.addAttribute("reply", reply);
        return screen(model, request, "ask", "ui.title.ask");
    }
    @GetMapping("/planned/{feature}")
    String planned(@PathVariable String feature, Model model,
                   jakarta.servlet.http.HttpServletRequest request) {
        if (feature.equals("saved")) return "redirect:/saved";
        if (feature.equals("updates")) return "redirect:/alerts";
        if (!PLANNED.containsKey(feature)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("feature", feature);
        return screen(model, request, "planned", PLANNED.get(feature));
    }
    @PostMapping("/data/{action:sample|refresh|demo}")
    String load(@PathVariable String action, @RequestParam(defaultValue = "false") boolean confirmSample,
                RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        if ((action.equals("sample") || action.equals("demo")) && catalog.status() != null
                && !confirmSample) {
            redirect.addFlashAttribute("error", messages.get(request, "ui.error.confirmSample"));
            return "redirect:/data";
        }
        if (action.equals("demo")) {
            return loadDemo(redirect, request);
        }
        try {
            var accepted = action.equals("sample") ? catalog.importSample() : catalog.refreshLive();
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.productsAccepted",
                    accepted.productCount(),
                    messages.get(request, action.equals("sample") ? "ui.error.sourceSample" : "ui.error.sourceLive")));
        } catch (RuntimeException exception) {
            // Keep provider and database details out of user-facing HTML.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Catalogue import failed", exception);
            redirect.addFlashAttribute("error", messages.get(request, "ui.error.importFailed"));
        }
        return "redirect:/data";
    }
    private String loadDemo(RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            var demo = demoHistory.seed();
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.demoLoaded",
                    demo.productCount(), demo.observationCount()));
        } catch (RuntimeException exception) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Demo data load failed", exception);
            redirect.addFlashAttribute("error", messages.get(request, "ui.error.demoFailed"));
        }
        return "redirect:/history";
    }
    @PostMapping("/list/items")
    String add(@RequestParam String code, @RequestParam int quantity,
               @RequestParam(defaultValue = "") String retailerCode, RedirectAttributes redirect,
               jakarta.servlet.http.HttpServletRequest request) {
        try {
            shopping.add(code, quantity, retailerCode);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.listAdded"));
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", messages.error(request, exception)); }
        return "redirect:/list";
    }
    @PostMapping("/list/items/{code}/update")
    String update(@PathVariable String code, @RequestParam int quantity,
                  @RequestParam(defaultValue = "false") boolean checked,
                  @RequestParam(required = false) String retailerCode, RedirectAttributes redirect,
                  jakarta.servlet.http.HttpServletRequest request) {
        try {
            if (retailerCode == null) shopping.update(code, quantity, checked);
            else shopping.update(code, quantity, checked, retailerCode);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.listUpdated"));
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", messages.error(request, exception)); }
        return "redirect:/list";
    }
    @PostMapping("/list/items/{code}/remove")
    String remove(@PathVariable String code, RedirectAttributes redirect,
                  jakarta.servlet.http.HttpServletRequest request) {
        shopping.remove(code);
        redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.listRemoved"));
        return "redirect:/list";
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, org.springframework.web.bind.MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String badInput(Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("error", messages.get(request, "ui.error.badInput"));
        return screen(model, request, "error", "ui.title.error");
    }
    private String screen(Model model, jakarta.servlet.http.HttpServletRequest request,
                          String scene, String titleKey) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", messages.get(request, titleKey));
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
        return DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm 'HKT'", Locale.ENGLISH)
                .withZone(ZoneId.of("Asia/Hong_Kong")).format(value);
    }
    /** Date only, Hong Kong time; used by the honest gap markers in history. */
    public String date(java.time.LocalDate value) {
        return value == null ? "" : DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH).format(value);
    }
}
