package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.i18n.AppMessages;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import org.springframework.context.i18n.LocaleContextHolder;
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
            "saved", "page.planned.saved", "updates", "page.planned.updates", "ask", "page.planned.ask");
    private final CatalogService catalog;
    private final ShoppingService shopping;

    public HelloController(CatalogService catalog, ShoppingService shopping) {
        this.catalog = catalog;
        this.shopping = shopping;
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
        return screen(model, "discover", "page.discover");
    }
    @GetMapping("/products/{code}")
    String compare(@PathVariable String code, Model model) {
        try { model.addAttribute("product", catalog.find(code)); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"); }
        return screen(model, "compare", "page.compare");
    }
    @GetMapping("/list")
    String list(Model model) {
        var items = shopping.items();
        model.addAttribute("items", items);
        model.addAttribute("summary", Summary.from(items));
        return screen(model, "list", "page.list");
    }
    @GetMapping("/data")
    String data(Model model) { return screen(model, "data", "page.data"); }
    @GetMapping("/planned/{feature}")
    String planned(@PathVariable String feature, Model model) {
        if (feature.equals("saved")) return "redirect:/saved";
        if (feature.equals("updates")) return "redirect:/alerts";
        if (feature.equals("ask")) return "redirect:/ask";
        if (!PLANNED.containsKey(feature)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("feature", feature);
        return screen(model, "planned", PLANNED.get(feature));
    }
    @PostMapping("/data/{action:sample|refresh}")
    String load(@PathVariable String action, @RequestParam(defaultValue = "false") boolean confirmSample, RedirectAttributes redirect) {
        if (action.equals("sample") && catalog.status() != null && !confirmSample) {
            redirect.addFlashAttribute("error", AppMessages.text("notice.sampleConfirm"));
            return "redirect:/data";
        }
        try {
            var accepted = action.equals("sample") ? catalog.importSample() : catalog.refreshLive();
            String note = AppMessages.text(action.equals("sample") ? "notice.importSampleNote" : "notice.importLiveNote");
            redirect.addFlashAttribute("notice", AppMessages.text("notice.importAccepted", accepted.productCount(), note));
        } catch (RuntimeException exception) {
            // Keep provider and database details out of user-facing HTML.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Catalogue import failed", exception);
            redirect.addFlashAttribute("error", AppMessages.text("notice.importFailed"));
        }
        return "redirect:/data";
    }
    @PostMapping("/list/items")
    String add(@RequestParam String code, @RequestParam int quantity,
               @RequestParam(defaultValue = "") String retailerCode, RedirectAttributes redirect) {
        try {
            shopping.add(code, quantity, retailerCode);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.itemAdded"));
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/list";
    }
    @PostMapping("/list/items/{code}/update")
    String update(@PathVariable String code, @RequestParam int quantity,
                  @RequestParam(defaultValue = "false") boolean checked,
                  @RequestParam(required = false) String retailerCode, RedirectAttributes redirect) {
        try {
            if (retailerCode == null) shopping.update(code, quantity, checked);
            else shopping.update(code, quantity, checked, retailerCode);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.listSaved"));
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/list";
    }
    @PostMapping("/list/items/{code}/remove")
    String remove(@PathVariable String code, RedirectAttributes redirect) {
        shopping.remove(code);
        redirect.addFlashAttribute("notice", AppMessages.text("notice.itemRemoved"));
        return "redirect:/list";
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, org.springframework.web.bind.MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String badInput(Model model) {
        model.addAttribute("error", AppMessages.text("error.input"));
        return screen(model, "error", "page.error");
    }
    private String screen(Model model, String scene, String title) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", AppMessages.text(title));
        return "market-desk";
    }
    public boolean hasListingPrice(ProductView product, String retailerCode) {
        return product.listings().stream().anyMatch(listing ->
                listing.retailerCode().equals(retailerCode) && listing.price() != null);
    }

    public String money(BigDecimal value) {
        if (value == null) return AppMessages.text("format.notListed");
        var format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-HK"));
        format.setCurrency(Currency.getInstance("HKD"));
        format.setMaximumFractionDigits(Math.max(2, value.stripTrailingZeros().scale()));
        return format.format(value);
    }
    public String time(Instant value) {
        return DateTimeFormatter.ofPattern(AppMessages.text("format.datetime"), LocaleContextHolder.getLocale())
                .withZone(ZoneId.of("Asia/Hong_Kong")).format(value);
    }
}
