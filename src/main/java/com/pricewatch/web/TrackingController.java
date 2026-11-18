package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.NotificationPolicy;
import com.pricewatch.tracking.TrackingService;
import com.pricewatch.notifications.AlertEventView;
import com.pricewatch.tracking.TrackingView;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.pricewatch.web.MilestoneMessages.inputError;
import static com.pricewatch.web.MilestoneMessages.text;

@Controller
public class TrackingController {
    private final CatalogService catalog;
    private final TrackingService tracking;
    private final AlertService alerts;
    private final NotificationPolicy notificationPolicy;
    private final HelloController format;
    private final Clock clock;
    private final MessageSource messages;

    public TrackingController(CatalogService catalog, TrackingService tracking, AlertService alerts,
                              NotificationPolicy notificationPolicy, HelloController format, Clock appClock,
                              MessageSource messages) {
        this.catalog = catalog;
        this.tracking = tracking;
        this.alerts = alerts;
        this.notificationPolicy = notificationPolicy;
        this.format = format;
        this.clock = appClock;
        this.messages = messages;
    }

    @GetMapping("/saved")
    public String saved(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("savedProducts", tracking.savedProducts());
        var allRetailers = tracking.allRetailersProducts();
        model.addAttribute("allRetailersProducts", allRetailers);
        model.addAttribute("allRetailerCodes", allRetailers.stream().map(TrackingService.AllRetailersView::code).toList());
        var periods = tracking.trackingPeriods();
        model.addAttribute("trackingPeriods", periods);
        model.addAttribute("productLabels", catalog.localizedLabels(Stream.concat(
                allRetailers.stream().map(TrackingService.AllRetailersView::code),
                periods.stream().map(TrackingView::productCode)).toList(), LocaleContextHolder.getLocale()));
        if (code != null && !code.isBlank()) {
            try {
                model.addAttribute("productToSave", catalog.find(code));
            } catch (IllegalArgumentException exception) {
                model.addAttribute("productLookupError", text(messages, "milestone.error.productMissing"));
            }
        }
        return screen(model, "saved", "Saved products & tracking");
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        var now = clock.instant();
        alerts.processDue(now);
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        var events = alerts.events(now);
        var periods = tracking.trackingPeriods().stream().filter(TrackingView::enabled).toList();
        var labels = catalog.localizedLabels(Stream.concat(events.stream().map(AlertEventView::productCode),
                periods.stream().map(TrackingView::productCode)).filter(Objects::nonNull).toList(),
                LocaleContextHolder.getLocale());
        model.addAttribute("alertEvents", events);
        model.addAttribute("eventMessages", events.stream().collect(Collectors.toMap(
                AlertEventView::id, event -> alertMessage(event, labels))));
        model.addAttribute("notificationSettings", notificationPolicy.settings());
        model.addAttribute("trackingPeriods", periods);
        model.addAttribute("productLabels", labels);
        return screen(model, "alerts", "Price alerts");
    }

    @PostMapping("/saved/save")
    public String save(@RequestParam String code, RedirectAttributes redirect) {
        try {
            tracking.save(code);
            redirect.addFlashAttribute("notice", text(messages, "saved.notice.saved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/saved/remove")
    public String removeSaved(@RequestParam String code, RedirectAttributes redirect) {
        tracking.removeSaved(code);
        redirect.addFlashAttribute("notice", text(messages, "saved.notice.removed"));
        return "redirect:/saved";
    }

    @PostMapping("/tracking/start")
    public String startTracking(@RequestParam String code, @RequestParam String retailerCode,
                                RedirectAttributes redirect) {
        try {
            tracking.start(code, retailerCode);
            redirect.addFlashAttribute("notice", text(messages, "saved.notice.started", retailerCode));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/start-all")
    public String startAll(@RequestParam String code, RedirectAttributes redirect) {
        try {
            tracking.startAll(code);
            redirect.addFlashAttribute("notice", text(messages, "saved.notice.startedAll"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/stop-all")
    public String stopAll(@RequestParam String code, RedirectAttributes redirect) {
        try {
            tracking.stopAll(code);
            redirect.addFlashAttribute("notice", text(messages, "saved.notice.stoppedAll"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/stop")
    public String stopTracking(@RequestParam long trackingId, RedirectAttributes redirect) {
        try {
            tracking.stop(trackingId);
            redirect.addFlashAttribute("notice", text(messages, "saved.notice.stopped"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/history/delete")
    public String deleteHistory(@RequestParam long trackingId, RedirectAttributes redirect) {
        try {
            tracking.deleteHistory(trackingId);
            redirect.addFlashAttribute("notice", text(messages, "saved.notice.historyDeleted"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/alerts/target")
    public String target(@RequestParam long trackingId,
                         @RequestParam(required = false) BigDecimal targetPrice,
                         RedirectAttributes redirect) {
        try {
            tracking.configureTarget(trackingId, targetPrice);
            redirect.addFlashAttribute("notice", targetPrice == null
                    ? text(messages, "alerts.notice.targetRemoved") : text(messages, "alerts.notice.targetSaved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/drop")
    public String drop(@RequestParam long trackingId,
                       @RequestParam(defaultValue = "false") boolean enabled,
                       RedirectAttributes redirect) {
        try {
            tracking.configureDrop(trackingId, enabled);
            redirect.addFlashAttribute("notice", enabled
                    ? text(messages, "alerts.notice.dropEnabled") : text(messages, "alerts.notice.dropDisabled"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/settings")
    public String settings(@RequestParam LocalTime quietStart, @RequestParam LocalTime quietEnd,
                           @RequestParam LocalTime summaryTime, RedirectAttributes redirect) {
        try {
            notificationPolicy.update(quietStart, quietEnd, summaryTime);
            redirect.addFlashAttribute("notice", text(messages, "alerts.notice.timingSaved"));
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("error", text(messages, "alerts.error.timing"));
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/events/{eventId}/read")
    public String read(@PathVariable long eventId, RedirectAttributes redirect) {
        try {
            alerts.markRead(eventId);
            redirect.addFlashAttribute("notice", text(messages, "alerts.notice.read"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/events/{eventId}/snooze")
    public String snooze(@PathVariable long eventId, RedirectAttributes redirect) {
        try {
            alerts.snooze(eventId);
            redirect.addFlashAttribute("notice", text(messages, "alerts.notice.snoozed"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/alerts";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badInput(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("error", text(messages, "alerts.error.input"));
        return screen(model, "error", text(messages, "error.input.title"));
    }

    private String alertMessage(AlertEventView event, Map<String, CatalogService.ProductLabel> labels) {
        var label = event.productCode() == null ? null : labels.get(event.productCode());
        String name = label == null ? event.productName() : label.name();
        return switch (event.type()) {
            case "TARGET" -> text(messages, "alerts.message.target", name, event.retailerCode());
            case "DROP" -> text(messages, "alerts.message.drop", name, event.retailerCode());
            case "SUMMARY" -> text(messages, event.summaryCount() == 1
                    ? "alerts.message.summaryOne" : "alerts.message.summaryMany", event.summaryCount().toString());
            default -> event.message();
        };
    }

    private static String screen(Model model, String scene, String title) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", title);
        return "market-desk";
    }
}
