package com.pricewatch.web;

import com.pricewatch.alerts.AlertService;
import com.pricewatch.alerts.persistence.AlertRuleEntity.Type;
import com.pricewatch.alerts.persistence.BudgetSettingsEntity.Period;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.shopping.BudgetService;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.persistence.WishlistEntryEntity;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Controller
public class HelloController {
    private static final Map<String, String> PLANNED = Map.of("ask", "Price assistant");
    private final CatalogService catalog;
    private final ShoppingService shopping;
    private final TrackingService tracking;
    private final AlertService alerts;
    private final BudgetService budget;
    private final ReminderService reminders;

    public HelloController(CatalogService catalog, ShoppingService shopping, TrackingService tracking,
                           AlertService alerts, BudgetService budget, ReminderService reminders) {
        this.catalog = catalog;
        this.shopping = shopping;
        this.tracking = tracking;
        this.alerts = alerts;
        this.budget = budget;
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
        model.addAttribute("saved", tracking.isSaved(code));
        model.addAttribute("tracked", tracking.isTracked(code));
        return screen(model, "compare", "Compare listed prices");
    }

    @GetMapping("/list")
    String list(Model model) {
        var items = shopping.items();
        model.addAttribute("items", items);
        model.addAttribute("summary", Summary.from(items));
        model.addAttribute("budget", budget.report());
        model.addAttribute("budgetSettings", budget.settings());
        return screen(model, "list", "My list");
    }

    @GetMapping("/saved")
    String saved(Model model) {
        Map<String, ProductView> products = new LinkedHashMap<>();
        Map<String, Boolean> tracked = new LinkedHashMap<>();
        for (WishlistEntryEntity entry : tracking.wishlistEntries()) {
            try {
                String code = entry.getProductCode();
                products.put(code, catalog.find(code));
                tracked.put(code, tracking.isTracked(code));
            } catch (IllegalArgumentException ignored) {
                // Product vanished from the catalogue; skip rendering it.
            }
        }
        model.addAttribute("savedProducts", products);
        model.addAttribute("tracked", tracked);
        model.addAttribute("trackingChoices", tracking.activeChoices());
        return screen(model, "saved", "Saved & tracking");
    }

    @GetMapping("/alerts")
    String alerts(Model model) {
        model.addAttribute("events", alerts.allEvents());
        model.addAttribute("held", alerts.heldEvents().size());
        model.addAttribute("summary", alerts.morningSummary());
        model.addAttribute("quiet", alerts.inQuietHours());
        model.addAttribute("rules", tracking.activeChoices());
        return screen(model, "alerts", "Alerts");
    }

    @GetMapping("/reminders")
    String reminders(Model model) {
        var all = reminders.all();
        java.util.Set<String> dueCodes = reminders.dueReminders().stream()
                .map(com.pricewatch.reminder.persistence.ReminderEntity::getProductCode)
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("reminders", all);
        model.addAttribute("dueCodes", dueCodes);
        model.addAttribute("dueCount", dueCodes.size());
        return screen(model, "reminders", "Reminders");
    }

    @GetMapping("/data")
    String data(Model model) { return screen(model, "data", "Price data & updates"); }

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

    @PostMapping("/list/budget")
    String saveBudget(@RequestParam Period period, @RequestParam BigDecimal plannedLimit, RedirectAttributes redirect) {
        try {
            budget.save(period, plannedLimit);
            redirect.addFlashAttribute("notice", "Planned budget saved on this device.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/list";
    }

    // --- J05: wishlist and tracking ---

    @PostMapping("/saved/{code}/save")
    String saveToWishlist(@PathVariable String code,
                          @RequestParam(defaultValue = "/saved") String back, RedirectAttributes redirect) {
        try {
            tracking.save(code);
            redirect.addFlashAttribute("notice", "Saved to your wishlist. Saving does not start tracking.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return safeRedirect(back);
    }

    @PostMapping("/saved/{code}/unsave")
    String unsave(@PathVariable String code,
                  @RequestParam(defaultValue = "/saved") String back, RedirectAttributes redirect) {
        tracking.unsave(code);
        redirect.addFlashAttribute("notice", "Removed from your wishlist.");
        return safeRedirect(back);
    }

    @PostMapping("/saved/{code}/track")
    String track(@PathVariable String code,
                 @RequestParam(defaultValue = "/saved") String back, RedirectAttributes redirect) {
        try {
            tracking.enableTracking(code);
            redirect.addFlashAttribute("notice", "Tracking on. Older prices are kept from the next successful import.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return safeRedirect(back);
    }

    @PostMapping("/saved/{code}/untrack")
    String untrack(@PathVariable String code,
                   @RequestParam(defaultValue = "/saved") String back, RedirectAttributes redirect) {
        tracking.disableTracking(code);
        redirect.addFlashAttribute("notice", "Tracking off. Existing history is kept.");
        return safeRedirect(back);
    }

    // --- J06: alerts ---

    @PostMapping("/alerts/rules")
    String createAlertRule(@RequestParam String rule, @RequestParam Type type,
                           @RequestParam(required = false) BigDecimal targetAmount,
                           RedirectAttributes redirect) {
        try {
            String[] parts = rule.split("\\|", 2);
            if (parts.length != 2) throw new IllegalArgumentException("Choose a tracked product and retailer.");
            alerts.createRule(parts[0], parts[1], type, targetAmount);
            redirect.addFlashAttribute("notice", "Alert rule added. It is checked after a successful import.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/{id}/snooze")
    String snoozeAlert(@PathVariable Long id, RedirectAttributes redirect) {
        alerts.snooze(id);
        redirect.addFlashAttribute("notice", "Alert snoozed for 24 hours.");
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/show")
    String showHeld(RedirectAttributes redirect) {
        int shown = alerts.showHeldNow();
        redirect.addFlashAttribute(shown > 0 ? "notice" : "error",
                shown > 0 ? shown + " new alert(s) shown." : "No alerts to show now (quiet hours or snoozed).");
        return "redirect:/alerts";
    }

    // --- J08: reminders ---

    @PostMapping("/reminders/{code}/create")
    String createReminder(@PathVariable String code, @RequestParam int quantity,
                          @RequestParam int intervalDays, RedirectAttributes redirect) {
        try {
            reminders.create(code, quantity, intervalDays);
            redirect.addFlashAttribute("notice", "Reminder set on this device.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/reminders";
    }

    @PostMapping("/reminders/{code}/add")
    String addReminderToList(@PathVariable String code, RedirectAttributes redirect) {
        try {
            reminders.addToList(code);
            redirect.addFlashAttribute("notice", "Added to My list and rescheduled.");
        } catch (IllegalArgumentException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/reminders";
    }

    @PostMapping("/reminders/{code}/clear")
    String clearReminder(@PathVariable String code, RedirectAttributes redirect) {
        reminders.clear(code);
        redirect.addFlashAttribute("notice", "Reminder cleared.");
        return "redirect:/reminders";
    }

    @PostMapping("/reminders/{code}/snooze")
    String snoozeReminder(@PathVariable String code, RedirectAttributes redirect) {
        reminders.snooze(code);
        redirect.addFlashAttribute("notice", "Reminder snoozed to tomorrow.");
        return "redirect:/reminders";
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

    private static String safeRedirect(String back) {
        // Only allow local paths so a crafted "back" cannot send the user off-site.
        return "redirect:" + (back != null && back.startsWith("/") && !back.startsWith("//") ? back : "/saved");
    }

    public String alertType(String type) {
        return "TARGET".equals(type) ? "Target met" : "Price drop";
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
