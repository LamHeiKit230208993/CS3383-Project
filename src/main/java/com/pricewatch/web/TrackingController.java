package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.NotificationPolicy;
import com.pricewatch.tracking.TrackingService;
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
import java.util.Objects;

@Controller
public class TrackingController {
    private final CatalogService catalog;
    private final TrackingService tracking;
    private final AlertService alerts;
    private final NotificationPolicy notificationPolicy;
    private final HelloController format;
    private final Clock clock;
    private final UiMessages messages;

    public TrackingController(CatalogService catalog, TrackingService tracking, AlertService alerts,
                              NotificationPolicy notificationPolicy, HelloController format, Clock appClock,
                              UiMessages messages) {
        this.catalog = catalog;
        this.tracking = tracking;
        this.alerts = alerts;
        this.notificationPolicy = notificationPolicy;
        this.format = format;
        this.clock = appClock;
        this.messages = messages;
    }

    @GetMapping("/saved")
    public String saved(@RequestParam(required = false) String code, Model model,
                        jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("savedProducts", tracking.savedProducts());
        var allRetailers = tracking.allRetailersProducts();
        model.addAttribute("allRetailersProducts", allRetailers);
        model.addAttribute("allRetailerCodes", allRetailers.stream().map(TrackingService.AllRetailersView::code).toList());
        var periods = tracking.trackingPeriods();
        model.addAttribute("trackingPeriods", periods);
        // One card per product: a product-wide track would otherwise repeat the
        // product once for every retailer it covers.
        var byProduct = new java.util.LinkedHashMap<String, java.util.List<com.pricewatch.tracking.TrackingView>>();
        for (var period : periods) {
            byProduct.computeIfAbsent(period.productCode(), key -> new java.util.ArrayList<>()).add(period);
        }
        model.addAttribute("trackingGroups", byProduct.values());
        if (code != null && !code.isBlank()) {
            try {
                model.addAttribute("productToSave", catalog.find(code));
            } catch (IllegalArgumentException exception) {
                model.addAttribute("productLookupError", messages.error(request, exception));
            }
        }
        return screen(model, request, "saved", "ui.title.saved");
    }

    @GetMapping("/alerts")
    public String alerts(Model model, jakarta.servlet.http.HttpServletRequest request) {
        var now = clock.instant();
        alerts.processDue(now);
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("alertEvents", alerts.events(now));
        model.addAttribute("notificationSettings", notificationPolicy.settings());
        model.addAttribute("trackingPeriods", tracking.trackingPeriods().stream()
                .filter(period -> period.enabled()).toList());
        return screen(model, request, "alerts", "ui.title.alerts");
    }

    @PostMapping("/saved/save")
    public String save(@RequestParam String code, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            tracking.save(code);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.savedToWishlist"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/saved/remove")
    public String removeSaved(@RequestParam String code, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        tracking.removeSaved(code);
        redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.removedFromWishlist"));
        return "redirect:/saved";
    }

    @PostMapping("/tracking/start")
    public String startTracking(@RequestParam String code, @RequestParam String retailerCode,
                                RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            tracking.start(code, retailerCode);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.trackingOn", retailerCode));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/start-all")
    public String startAll(@RequestParam String code, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            tracking.startAll(code);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.trackingAll"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/stop-all")
    public String stopAll(@RequestParam String code, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            tracking.stopAll(code);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.trackingAllStopped"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/stop")
    public String stopTracking(@RequestParam long trackingId, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            tracking.stop(trackingId);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.trackingStopped"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/history/delete")
    public String deleteHistory(@RequestParam long trackingId, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            tracking.deleteHistory(trackingId);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.historyDeleted"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/saved";
    }

    @PostMapping("/alerts/target")
    public String target(@RequestParam long trackingId,
                         @RequestParam(required = false) BigDecimal targetPrice,
                         RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            tracking.configureTarget(trackingId, targetPrice);
            redirect.addFlashAttribute("notice", messages.get(request,
                    targetPrice == null ? "ui.notice.targetRemoved" : "ui.notice.targetSaved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/drop")
    public String drop(@RequestParam long trackingId,
                       @RequestParam(defaultValue = "false") boolean enabled,
                       RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            tracking.configureDrop(trackingId, enabled);
            redirect.addFlashAttribute("notice", messages.get(request,
                    enabled ? "ui.notice.dropEnabled" : "ui.notice.dropDisabled"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/settings")
    public String settings(@RequestParam LocalTime quietStart, @RequestParam LocalTime quietEnd,
                           @RequestParam LocalTime summaryTime, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            notificationPolicy.update(quietStart, quietEnd, summaryTime);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.timingSaved"));
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("error", messages.get(request, "ui.error.alertTiming"));
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/events/{eventId}/read")
    public String read(@PathVariable long eventId, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            alerts.markRead(eventId);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.markedRead"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/events/{eventId}/snooze")
    public String snooze(@PathVariable long eventId, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            alerts.snooze(eventId);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.snoozed"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/alerts";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badInput(Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("error", messages.get(request, "ui.error.trackingSelection"));
        return screen(model, request, "error", "ui.title.error");
    }

    private String screen(Model model, jakarta.servlet.http.HttpServletRequest request,
                          String scene, String titleKey) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", messages.get(request, titleKey));
        return "market-desk";
    }
}
