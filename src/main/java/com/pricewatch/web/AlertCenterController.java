package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.notifications.AlertEventView;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.ProductTargetEventView;
import com.pricewatch.notifications.ProductTargetService;
import com.pricewatch.notifications.WatchRuleService;
import com.pricewatch.notifications.WatchRuleView;
import com.pricewatch.notifications.WatchPriceKind;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.tracking.TrackingView;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.pricewatch.web.MilestoneMessages.inputError;
import static com.pricewatch.web.MilestoneMessages.text;

@Controller
class AlertCenterController {
    private static final ZoneId HONG_KONG = ZoneId.of("Asia/Hong_Kong");
    private static final DateTimeFormatter TIMELINE_CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private final CatalogService catalog;
    private final TrackingService tracking;
    private final AlertService legacyAlerts;
    private final ProductTargetService priceTargets;
    private final WatchRuleService watchRules;
    private final HelloController format;
    private final MessageSource messages;
    private final Clock clock;

    AlertCenterController(CatalogService catalog, TrackingService tracking, AlertService legacyAlerts,
                          ProductTargetService priceTargets, WatchRuleService watchRules,
                          HelloController format, MessageSource messages, Clock clock) {
        this.catalog = catalog;
        this.tracking = tracking;
        this.legacyAlerts = legacyAlerts;
        this.priceTargets = priceTargets;
        this.watchRules = watchRules;
        this.format = format;
        this.messages = messages;
        this.clock = clock;
    }

    @GetMapping("/alerts")
    String alerts(@RequestParam(defaultValue = "history") String tab, Model model, Locale locale) {
        watchRules.checkDue();
        boolean currentRules = "rules".equals(tab);
        model.addAttribute("currentRules", currentRules);
        if (currentRules) {
            model.addAttribute("ruleProducts", ruleProducts(locale));
        } else {
            addTimeline(model, locale);
        }
        return screen(model, "alerts", "Alerts");
    }

    private void addTimeline(Model model, Locale locale) {
        Instant now = clock.instant();
        var history = new ArrayList<TimelineEntry>();
        for (var legacy : legacyAlerts.events(now)) {
            if (legacy.productCode() != null) {
                history.add(new TimelineEntry(legacy.createdAt(), legacy.productCode(), legacy, null));
            }
        }
        for (var price : priceTargets.events(now)) {
            history.add(new TimelineEntry(price.createdAt(), price.productCode(), null, price));
        }
        history.sort(Comparator.comparing(TimelineEntry::createdAt).reversed());

        var labelCodes = new TreeSet<String>();
        history.forEach(item -> labelCodes.add(item.productCode()));
        model.addAttribute("productLabels", catalog.localizedLabels(labelCodes, locale));
        var visible = new ArrayList<>(history.stream().limit(100).toList());
        history.stream().skip(100).filter(item -> item.price() != null && item.price().activeReminder())
                .forEach(visible::add);
        model.addAttribute("timelineDays", timelineDays(visible, locale));
    }

    @GetMapping("/alerts/toasts")
    ResponseEntity<List<AlertToast>> toasts(Locale locale) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(Duration.ofMinutes(2));
        var recent = new ArrayList<AlertToast>();
        for (var alert : legacyAlerts.events(now)) {
            if (alert.productCode() == null || alert.read() || !alert.available()
                    || alert.createdAt().isBefore(cutoff) || alert.price() == null) continue;
            boolean drop = "DROP".equals(alert.type());
            recent.add(new AlertToast("retailer:" + alert.id(), alert.productCode(),
                    text(messages, drop ? "toast.drop" : "toast.target"),
                    toastPrice(alert.retailerCode(), alert.previousPrice(), alert.price()),
                    alert.createdAt(), false));
        }
        for (var alert : priceTargets.events(now)) {
            if (alert.read() || !alert.available() || alert.createdAt().isBefore(cutoff)) continue;
            boolean reminder = alert.target() == null;
            String title = reminder ? "alertCenter.time"
                    : alert.priceKind() == WatchPriceKind.TARGET ? "toast.target" : "toast.drop";
            var retailer = alert.retailers().stream().findFirst().orElse(null);
            String detail = retailer == null ? "" : toastPrice(retailer.retailerCode(), retailer.previousPrice(), retailer.price());
            recent.add(new AlertToast("product:" + alert.id(), alert.productCode(), text(messages, title),
                    detail, alert.createdAt(), reminder));
        }
        var latest = recent.stream().sorted(Comparator.comparing(AlertToast::createdAt).reversed()).limit(5).toList();
        var labels = catalog.localizedLabels(latest.stream().map(AlertToast::product).distinct().toList(), locale);
        var toasts = latest.stream().map(alert -> new AlertToast(alert.id(), name(labels, alert.product()),
                alert.title(), alert.detail(), alert.createdAt(), alert.reminder())).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(toasts);
    }

    private String toastPrice(String retailer, BigDecimal previous, BigDecimal current) {
        String price = previous == null ? format.money(current) : format.money(previous) + " → " + format.money(current);
        return retailer == null ? price : retailer + " · " + price;
    }

    record AlertToast(String id, String product, String title, String detail, Instant createdAt, boolean reminder) {}

    private List<RuleProduct> ruleProducts(Locale locale) {
        var existingRules = watchRules.allRules().stream().collect(Collectors.groupingBy(WatchRuleView::productCode));
        var targets = priceTargets.rules().stream().collect(Collectors.toMap(ProductTargetService.RuleView::productCode, rule -> rule));
        var retailerRules = tracking.legacyRules().stream().collect(Collectors.groupingBy(TrackingView::productCode));
        var codes = new TreeSet<>(existingRules.keySet());
        codes.addAll(targets.keySet());
        codes.addAll(retailerRules.keySet());
        var labels = catalog.localizedLabels(codes, locale);
        return codes.stream()
                .map(code -> new RuleProduct(code, name(labels, code), existingRules.getOrDefault(code, List.of()),
                        targets.get(code), retailerRules.getOrDefault(code, List.of())))
                .sorted(Comparator.comparing(RuleProduct::name)).toList();
    }

    @GetMapping("/alerts/rules")
    String rules(@RequestParam String code, @RequestParam(defaultValue = "") String from, Model model) {
        ProductView product;
        try {
            product = catalog.find(code);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("product", product);
        model.addAttribute("rulesFrom", from);
        model.addAttribute("wishlistTracking", tracking.savedProducts().stream()
                .anyMatch(saved -> saved.product().code().equals(code)));
        model.addAttribute("productTarget", priceTargets.rule(code).orElse(null));
        model.addAttribute("watchRules", watchRules.rules(code));
        model.addAttribute("legacyRules", tracking.legacyRules().stream()
                .filter(period -> period.productCode().equals(code)).toList());
        return screen(model, "rules", "Alert rules");
    }

    @PostMapping("/alerts/product-target")
    String saveTarget(@RequestParam String code, @RequestParam String targetPrice, RedirectAttributes redirect) {
        try {
            priceTargets.setTarget(code, new BigDecimal(targetPrice.strip()));
            redirect.addFlashAttribute("notice", text(messages, "alerts.notice.targetSaved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", text(messages, "alertRules.invalidTarget"));
        }
        return toRules(code, redirect);
    }

    @PostMapping("/alerts/product-target/delete")
    String deleteTarget(@RequestParam String code, @RequestParam(defaultValue = "") String from, RedirectAttributes redirect) {
        priceTargets.deleteTarget(code);
        redirect.addFlashAttribute("notice", text(messages, "alerts.notice.targetRemoved"));
        return toRules(code, from, redirect);
    }

    @PostMapping("/alerts/product-target/enabled")
    String enableTarget(@RequestParam String code, @RequestParam boolean enabled,
                        @RequestParam(defaultValue = "") String from, RedirectAttributes redirect) {
        return toggleLegacy(code, from, enabled, redirect, () -> priceTargets.setEnabled(code, enabled));
    }

    @PostMapping("/alerts/retailer-rule/enabled")
    String enableRetailerRule(@RequestParam String code, @RequestParam long id, @RequestParam boolean enabled,
                              @RequestParam(defaultValue = "") String from, RedirectAttributes redirect) {
        return toggleLegacy(code, from, enabled, redirect, () -> tracking.setLegacyAlertsEnabled(code, id, enabled));
    }

    @PostMapping("/alerts/retailer-rule/delete")
    String deleteRetailerRule(@RequestParam String code, @RequestParam long id,
                              @RequestParam(defaultValue = "") String from, RedirectAttributes redirect) {
        try {
            tracking.deleteLegacyAlerts(code, id);
            redirect.addFlashAttribute("notice", text(messages, "watchRules.removed"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", text(messages, "watchRules.missing"));
        }
        return toRules(code, from, redirect);
    }

    private String toggleLegacy(String code, String from, boolean enabled, RedirectAttributes redirect, Runnable toggle) {
        try {
            toggle.run();
            redirect.addFlashAttribute("notice", text(messages, enabled ? "watchEditor.enabled" : "watchEditor.disabled"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", text(messages, "watchRules.missing"));
        }
        return toRules(code, from, redirect);
    }

    @PostMapping("/alerts/watch-rules/save")
    String saveWatchRule(@RequestParam String code, @RequestParam(required = false) Long id,
                         @RequestParam(required = false) String targetPrice, @RequestParam(required = false) String intervalDays,
                         @RequestParam(required = false) String condition, @RequestParam(required = false) String startAt,
                         @RequestParam(required = false) String priceKind, @RequestParam(required = false) String quantity,
                         @RequestParam(required = false) String retailerCode,
                         @RequestParam(required = false) String repeat, @RequestParam(defaultValue = "") String from,
                         RedirectAttributes redirect) {
        try {
            if ("migrated-time".equals(condition)) {
                if (id == null) throw new IllegalArgumentException("Missing reminder rule");
                watchRules.updateMigratedTimeRule(code, id, Integer.parseInt(intervalDays), Integer.parseInt(quantity), retailerCode);
            } else if (condition == null || condition.equals("legacy")) {
                var interval = intervalDays == null ? null : Integer.valueOf(intervalDays.strip());
                watchRules.save(code, id, price(targetPrice), interval);
            } else {
                switch (condition) {
                    case "price" -> savePriceRule(code, id, price(targetPrice), null, repeat, priceKind);
                    case "time" -> watchRules.save(code, id, null, schedule(startAt), repeat);
                    case "combined" -> savePriceRule(code, id, price(targetPrice), schedule(startAt), repeat, priceKind);
                    default -> throw new IllegalArgumentException("Unknown alert condition");
                }
            }
            redirect.addFlashAttribute("notice", text(messages, "watchRules.saved"));
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            redirect.addFlashAttribute("error", text(messages, "watchEditor.invalid"));
        }
        return toRules(code, from, redirect);
    }

    private void savePriceRule(String code, Long id, BigDecimal amount, LocalDateTime date, String repeat, String priceKind) {
        var kind = WatchPriceKind.parse(priceKind);
        if (kind == WatchPriceKind.TARGET) watchRules.save(code, id, amount, date, repeat);
        else watchRules.save(code, id, amount, date, repeat, kind);
    }

    private static BigDecimal price(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Missing target price");
        }
        return new BigDecimal(input.strip());
    }

    private static LocalDateTime schedule(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Missing start date");
        }
        return LocalDateTime.parse(input.strip());
    }

    @PostMapping("/alerts/watch-rules/enabled")
    String setWatchRuleEnabled(@RequestParam String code, @RequestParam long id, @RequestParam boolean enabled,
                               @RequestParam(defaultValue = "") String from, RedirectAttributes redirect) {
        try {
            watchRules.setEnabled(code, id, enabled);
            redirect.addFlashAttribute("notice", text(messages,
                    enabled ? "watchEditor.enabled" : "watchEditor.disabled"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", text(messages, "watchRules.missing"));
        }
        return toRules(code, from, redirect);
    }

    @PostMapping("/alerts/watch-rules/delete")
    String deleteWatchRule(@RequestParam String code, @RequestParam long id,
                           @RequestParam(defaultValue = "") String from, RedirectAttributes redirect) {
        try {
            watchRules.delete(code, id);
            redirect.addFlashAttribute("notice", text(messages, "watchRules.removed"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", text(messages, "watchRules.missing"));
        }
        return toRules(code, from, redirect);
    }

    @PostMapping("/alerts/product-events/{id}/read")
    String readTarget(@PathVariable long id, RedirectAttributes redirect) {
        return eventAction(redirect, () -> {
            if (!watchRules.dismissCartReminder(id)) priceTargets.markRead(id);
        }, "alerts.notice.read");
    }

    @PostMapping("/alerts/product-events/{id}/snooze")
    String snoozeTarget(@PathVariable long id, RedirectAttributes redirect) {
        return eventAction(redirect, () -> {
            if (!watchRules.snoozeCartReminder(id)) priceTargets.snooze(id);
        }, "alerts.notice.snoozed");
    }

    @PostMapping("/alerts/product-events/{id}/add")
    String addReminderToCart(@PathVariable long id, RedirectAttributes redirect) {
        return eventAction(redirect, () -> watchRules.addReminderToCart(id), "reminders.notice.added");
    }

    @GetMapping("/reminders")
    String oldReminders() {
        return "redirect:/alerts";
    }

    private String eventAction(RedirectAttributes redirect, Runnable action, String success) {
        try {
            action.run();
            redirect.addFlashAttribute("notice", text(messages, success));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/alerts";
    }

    private String screen(Model model, String scene, String title) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", title);
        model.addAttribute("format", format);
        model.addAttribute("importStatus", catalog.status());
        return "market-desk";
    }

    private static String toRules(String code, RedirectAttributes redirect) {
        return toRules(code, "", redirect);
    }

    private static String toRules(String code, String from, RedirectAttributes redirect) {
        if (from.equals("center")) {
            return "redirect:/alerts?tab=rules";
        }
        redirect.addAttribute("code", code);
        if (from.equals("saved") || from.equals("rules")) {
            redirect.addAttribute("from", from);
        }
        return "redirect:/alerts/rules";
    }

    private static String name(Map<String, CatalogService.ProductLabel> labels, String code) {
        return labels.containsKey(code) ? labels.get(code).name() : code;
    }

    static List<TimelineDay> timelineDays(List<TimelineEntry> entries, Locale locale) {
        var byDate = new LinkedHashMap<LocalDate, List<TimelineEntry>>();
        for (var entry : entries) {
            var day = entry.createdAt().atZone(HONG_KONG).toLocalDate();
            byDate.computeIfAbsent(day, key -> new ArrayList<>()).add(entry);
        }
        var dateLabel = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale);
        return byDate.entrySet().stream()
                .map(day -> new TimelineDay(day.getKey(), dateLabel.format(day.getKey()), List.copyOf(day.getValue())))
                .toList();
    }

    public record TimelineEntry(Instant createdAt, String productCode, AlertEventView legacy,
                                ProductTargetEventView price) {
        public String clockTime() {
            return TIMELINE_CLOCK.withZone(HONG_KONG).format(createdAt);
        }
    }
    public record TimelineDay(LocalDate date, String label, List<TimelineEntry> entries) {
        public List<TimelineGroup> groups() {
            var byTrigger = new LinkedHashMap<TimelineTrigger, List<TimelineEntry>>();
            for (var entry : entries) {
                var key = new TimelineTrigger(entry.productCode(), entry.createdAt());
                byTrigger.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
            }
            return byTrigger.values().stream()
                    .map(group -> new TimelineGroup(group.getFirst(), List.copyOf(group)))
                    .toList();
        }
    }

    private record TimelineTrigger(String productCode, Instant triggeredAt) {}
    public record TimelineGroup(TimelineEntry first, List<TimelineEntry> entries) {}
    public record RuleProduct(String code, String name, List<WatchRuleView> rules, ProductTargetService.RuleView target,
                              List<TrackingView> retailers) {}
}
