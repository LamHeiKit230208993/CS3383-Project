package com.pricewatch.web;

import com.pricewatch.alerts.AlertService;
import com.pricewatch.alerts.persistence.AlertRuleEntity;
import com.pricewatch.budget.BudgetService;
import com.pricewatch.reminders.ReminderService;
import com.pricewatch.budget.persistence.BudgetSettingsEntity;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import com.pricewatch.tracking.TrackingService;
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
            "ask", "Price assistant");
    private final CatalogService catalog;
    private final ShoppingService shopping;
    private final TrackingService tracking;
    private final BudgetService budget;
    private final AlertService alerts;
    private final ReminderService reminders;

    public HelloController(CatalogService catalog, ShoppingService shopping, TrackingService tracking,
                           BudgetService budget, AlertService alerts, ReminderService reminders) {
        this.catalog = catalog;
        this.shopping = shopping;
        this.tracking = tracking;
        this.budget = budget;
        this.alerts = alerts;
        this.reminders = reminders;
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
        return screen(model, "discover", "Discover");
    }
    @GetMapping("/products/{code}")
    String compare(@PathVariable String code, Model model) {
        try { model.addAttribute("product", catalog.find(code)); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"); }
        return screen(model, "compare", "Compare listed prices");
    }
    @GetMapping("/list")
    String list(Model model) {
        var items = shopping.items();
        model.addAttribute("items", items);
        model.addAttribute("summary", Summary.from(items));
        model.addAttribute("budget", budget.view());
        return screen(model, "list", "My list");
    }
    @PostMapping("/budget")
    String setBudget(@RequestParam String period, @RequestParam BigDecimal limit, RedirectAttributes redirect) {
        try {
            BudgetSettingsEntity.Period parsed;
            try {
                parsed = BudgetSettingsEntity.Period.valueOf(period.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Choose a weekly or monthly budget.");
            }
            budget.setBudget(parsed, limit);
            redirect.addFlashAttribute("notice", "Planned budget saved. This is an estimate, not actual spending.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/list";
    }
    @PostMapping("/budget/clear")
    String clearBudget(RedirectAttributes redirect) {
        budget.clearBudget();
        redirect.addFlashAttribute("notice", "Planned budget cleared.");
        return "redirect:/list";
    }
    @GetMapping("/data")
    String data(Model model) { return screen(model, "data", "Price data & updates"); }    @GetMapping("/saved")
    String saved(Model model) {
        model.addAttribute("savedProducts", tracking.savedProducts());
        model.addAttribute("trackedProducts", tracking.trackedProducts());
        return screen(model, "saved", "Saved & tracked");
    }
    @PostMapping("/saved/{code}/save")
    String saveProduct(@PathVariable String code, RedirectAttributes redirect) {
        try {
            tracking.save(code);
            redirect.addFlashAttribute("notice", "Saved. Saving does not start tracking.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/saved";
    }
    @PostMapping("/saved/{code}/unsave")
    String unsaveProduct(@PathVariable String code, RedirectAttributes redirect) {
        tracking.unsave(code);
        redirect.addFlashAttribute("notice", "Removed from saved. Any tracking history is kept.");
        return "redirect:/saved";
    }
    @PostMapping("/tracking/{code}/enable")
    String enableTracking(@PathVariable String code, RedirectAttributes redirect) {
        try {
            tracking.enableTracking(code);
            redirect.addFlashAttribute("notice", "Tracking on. Prices are recorded for this product's retailers on each import.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/saved";
    }
    @PostMapping("/tracking/{code}/disable")
    String disableTracking(@PathVariable String code, RedirectAttributes redirect) {
        tracking.disableTracking(code);
        redirect.addFlashAttribute("notice", "Tracking off. Existing history is kept.");
        return "redirect:/saved";
    }
    @GetMapping("/alerts")
    String alerts(Model model) {
        alerts.evaluateAll();
        model.addAttribute("alerts", alerts.view());
        model.addAttribute("trackedProducts", tracking.trackedProducts());
        return screen(model, "alerts", "Alerts");
    }
    @PostMapping("/alerts/rules")
    String createAlertRule(@RequestParam String productCode, @RequestParam String retailerCode,
                           @RequestParam String type, @RequestParam(required = false) BigDecimal target,
                           RedirectAttributes redirect) {
        try {
            AlertRuleEntity.Type parsed;
            try {
                parsed = AlertRuleEntity.Type.valueOf(type.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Choose a target or drop alert.");
            }
            alerts.createRule(productCode, retailerCode, parsed, target);
            redirect.addFlashAttribute("notice", "Alert rule saved. It is checked after each valid import while the app runs.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/alerts";
    }
    @PostMapping("/alerts/rules/{id}/delete")
    String deleteAlertRule(@PathVariable Long id, RedirectAttributes redirect) {
        alerts.deleteRule(id);
        redirect.addFlashAttribute("notice", "Alert rule removed.");
        return "redirect:/alerts";
    }
    @PostMapping("/alerts/events/{id}/acknowledge")
    String acknowledgeAlert(@PathVariable Long id, RedirectAttributes redirect) {
        alerts.acknowledge(id);
        redirect.addFlashAttribute("notice", "Alert acknowledged.");
        return "redirect:/alerts";
    }
    @PostMapping("/alerts/events/{id}/snooze")
    String snoozeAlert(@PathVariable Long id, RedirectAttributes redirect) {
        alerts.snooze(id);
        redirect.addFlashAttribute("notice", "Alert snoozed for 8 hours.");
        return "redirect:/alerts";
    }
    @GetMapping("/reminders")
    String reminders(Model model) {
        model.addAttribute("reminders", reminders.reminders());
        return screen(model, "reminders", "Reminders");
    }
    @PostMapping("/reminders")
    String createReminder(@RequestParam String code, @RequestParam int quantity,
                          @RequestParam int intervalDays, @RequestParam int snoozeDays,
                          RedirectAttributes redirect) {
        try {
            reminders.create(code, quantity, intervalDays, snoozeDays);
            redirect.addFlashAttribute("notice", "Reminder set. These run only while the app is open.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/reminders";
    }
    @PostMapping("/reminders/{id}/add-to-list")
    String reminderAddToList(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            reminders.addToList(id);
            redirect.addFlashAttribute("notice", "Added to My list.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/reminders";
    }
    @PostMapping("/reminders/{id}/clear")
    String reminderClear(@PathVariable Long id, RedirectAttributes redirect) {
        reminders.clear(id);
        redirect.addFlashAttribute("notice", "Reminder cleared.");
        return "redirect:/reminders";
    }
    @PostMapping("/reminders/{id}/snooze")
    String reminderSnooze(@PathVariable Long id, RedirectAttributes redirect) {
        reminders.snooze(id);
        redirect.addFlashAttribute("notice", "Reminder snoozed.");
        return "redirect:/reminders";
    }
    @GetMapping("/planned/{feature}")
    String planned(@PathVariable String feature, Model model) {
        if (!PLANNED.containsKey(feature)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("feature", feature);
        return screen(model, "planned", PLANNED.get(feature));
    }
    @PostMapping("/data/{action:sample|refresh}")
    String load(@PathVariable String action, @RequestParam(defaultValue = "false") boolean confirmSample, RedirectAttributes redirect) {
        if (action.equals("sample") && catalog.status() != null && !confirmSample) {
            redirect.addFlashAttribute("error", "Confirm that you want to replace the accepted catalogue with the historical sample.");
            return "redirect:/data";
        }
        try {
            var accepted = action.equals("sample") ? catalog.importSample() : catalog.refreshLive();
            redirect.addFlashAttribute("notice", accepted.productCount() + " products accepted. "
                    + (action.equals("sample") ? "This is a historical sample, not current prices." : "Live source downloaded; source price date is unknown."));
            String historyWarning = catalog.takeHistoryWarning();
            if (historyWarning != null) {
                redirect.addFlashAttribute("error", historyWarning);
            }
        } catch (RuntimeException exception) {
            // Keep provider and database details out of user-facing HTML.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Catalogue import failed", exception);
            redirect.addFlashAttribute("error", "Import failed. Your last accepted catalogue and shopping list were kept. Check the connection or try again.");
        }
        return "redirect:/data";
    }
    @PostMapping("/list/items")
    String add(@RequestParam String code, @RequestParam int quantity,
               @RequestParam(defaultValue = "") String retailerCode, RedirectAttributes redirect) {
        try {
            shopping.add(code, quantity, retailerCode);
            redirect.addFlashAttribute("notice", "Item added to your saved list.");
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
            redirect.addFlashAttribute("notice", "List changes saved on this device.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/list";
    }
    @PostMapping("/list/items/{code}/remove")
    String remove(@PathVariable String code, RedirectAttributes redirect) {
        shopping.remove(code);
        redirect.addFlashAttribute("notice", "Item removed from your list.");
        return "redirect:/list";
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, org.springframework.web.bind.MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String badInput(Model model) {
        model.addAttribute("error", "Check your input. Use a whole quantity from 1 to 999 and a valid page number.");
        return screen(model, "error", "Check your input");
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
        return DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm 'HKT'", Locale.ENGLISH)
                .withZone(ZoneId.of("Asia/Hong_Kong")).format(value);
    }
}
