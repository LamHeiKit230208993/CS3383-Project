package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.notifications.AlertEventView;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.ProductTargetEventView;
import com.pricewatch.notifications.ProductTargetService;
import com.pricewatch.reminder.ReminderOccurrenceView;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.tracking.TrackingService;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import static com.pricewatch.web.MilestoneMessages.inputError;
import static com.pricewatch.web.MilestoneMessages.text;

@Controller
class AlertCenterController {
    private final CatalogService catalog;
    private final TrackingService tracking;
    private final AlertService legacyAlerts;
    private final ProductTargetService priceTargets;
    private final ReminderService reminders;
    private final HelloController format;
    private final MessageSource messages;
    private final Clock clock;

    AlertCenterController(CatalogService catalog, TrackingService tracking, AlertService legacyAlerts,
                          ProductTargetService priceTargets, ReminderService reminders, HelloController format,
                          MessageSource messages, Clock clock) {
        this.catalog = catalog;
        this.tracking = tracking;
        this.legacyAlerts = legacyAlerts;
        this.priceTargets = priceTargets;
        this.reminders = reminders;
        this.format = format;
        this.messages = messages;
        this.clock = clock;
    }

    @GetMapping("/alerts")
    String alerts(Model model, Locale locale) {
        reminders.recordDue();
        Instant now = clock.instant();
        var history = new ArrayList<TimelineEntry>();
        for (var legacy : legacyAlerts.events(now)) {
            if (legacy.productCode() != null) {
                history.add(new TimelineEntry(legacy.createdAt(), legacy.productCode(), legacy, null, null));
            }
        }
        for (var price : priceTargets.events(now)) {
            history.add(new TimelineEntry(price.createdAt(), price.productCode(), null, price, null));
        }
        for (var time : reminders.occurrences(now)) {
            history.add(new TimelineEntry(time.createdAt(), time.productCode(), null, null, time));
        }
        history.sort(Comparator.comparing(TimelineEntry::createdAt).reversed());

        var ruleCodes = new TreeSet<String>();
        tracking.savedProducts().forEach(saved -> ruleCodes.add(saved.product().code()));
        reminders.overview().reminders().forEach(reminder -> ruleCodes.add(reminder.productCode()));
        tracking.activeLegacyRules().stream()
                .forEach(period -> ruleCodes.add(period.productCode()));
        var labelCodes = new TreeSet<>(ruleCodes);
        history.forEach(item -> labelCodes.add(item.productCode()));
        var labels = catalog.localizedLabels(labelCodes, locale);
        model.addAttribute("productLabels", labels);
        var visible = new ArrayList<>(history.stream().limit(100).toList());
        history.stream().skip(100).filter(item -> item.time() != null && item.time().active()).forEach(visible::add);
        model.addAttribute("history", visible);
        model.addAttribute("ruleProducts", ruleCodes.stream().map(code -> new RuleProduct(code, name(labels, code)))
                .sorted(Comparator.comparing(RuleProduct::name)).toList());
        return screen(model, "alerts", "Alerts");
    }

    @GetMapping("/alerts/rules")
    String rules(@RequestParam String code, Model model) {
        ProductView product;
        try {
            product = catalog.find(code);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var target = priceTargets.target(code).orElse(null);
        model.addAttribute("product", product);
        model.addAttribute("wishlistTracking", tracking.savedProducts().stream()
                .anyMatch(saved -> saved.product().code().equals(code)));
        model.addAttribute("productTarget", target == null ? "" : target.toPlainString());
        model.addAttribute("timeRules", reminders.overview().reminders().stream()
                .filter(rule -> rule.productCode().equals(code)).toList());
        model.addAttribute("legacyRules", tracking.activeLegacyRules().stream()
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
    String deleteTarget(@RequestParam String code, RedirectAttributes redirect) {
        priceTargets.deleteTarget(code);
        redirect.addFlashAttribute("notice", text(messages, "alerts.notice.targetRemoved"));
        return toRules(code, redirect);
    }

    @PostMapping("/alerts/product-events/{id}/read")
    String readTarget(@PathVariable long id, RedirectAttributes redirect) {
        return eventAction(redirect, () -> priceTargets.markRead(id), "alerts.notice.read");
    }

    @PostMapping("/alerts/product-events/{id}/snooze")
    String snoozeTarget(@PathVariable long id, RedirectAttributes redirect) {
        return eventAction(redirect, () -> priceTargets.snooze(id), "alerts.notice.snoozed");
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
        redirect.addAttribute("code", code);
        return "redirect:/alerts/rules";
    }

    private static String name(Map<String, CatalogService.ProductLabel> labels, String code) {
        return labels.containsKey(code) ? labels.get(code).name() : code;
    }

    public record TimelineEntry(Instant createdAt, String productCode, AlertEventView legacy,
                                ProductTargetEventView price, ReminderOccurrenceView time) {}
    public record RuleProduct(String code, String name) {}
}
