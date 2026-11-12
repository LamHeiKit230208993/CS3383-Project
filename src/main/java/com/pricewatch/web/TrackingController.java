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

    public TrackingController(CatalogService catalog, TrackingService tracking, AlertService alerts,
                              NotificationPolicy notificationPolicy, HelloController format, Clock appClock) {
        this.catalog = catalog;
        this.tracking = tracking;
        this.alerts = alerts;
        this.notificationPolicy = notificationPolicy;
        this.format = format;
        this.clock = appClock;
    }

    @GetMapping("/saved")
    public String saved(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("savedProducts", tracking.savedProducts());
        var allRetailers = tracking.allRetailersProducts();
        model.addAttribute("allRetailersProducts", allRetailers);
        model.addAttribute("allRetailerCodes", allRetailers.stream().map(TrackingService.AllRetailersView::code).toList());
        model.addAttribute("trackingPeriods", tracking.trackingPeriods());
        if (code != null && !code.isBlank()) {
            try {
                model.addAttribute("productToSave", catalog.find(code));
            } catch (IllegalArgumentException exception) {
                model.addAttribute("productLookupError", exception.getMessage());
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
        model.addAttribute("alertEvents", alerts.events(now));
        model.addAttribute("notificationSettings", notificationPolicy.settings());
        model.addAttribute("trackingPeriods", tracking.trackingPeriods().stream()
                .filter(period -> period.enabled()).toList());
        return screen(model, "alerts", "Price alerts");
    }

    @PostMapping("/saved/save")
    public String save(@RequestParam String code, RedirectAttributes redirect) {
        try {
            tracking.save(code);
            redirect.addFlashAttribute("notice", "Product saved to your wishlist.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/saved/remove")
    public String removeSaved(@RequestParam String code, RedirectAttributes redirect) {
        tracking.removeSaved(code);
        redirect.addFlashAttribute("notice", "Product removed from your wishlist.");
        return "redirect:/saved";
    }

    @PostMapping("/tracking/start")
    public String startTracking(@RequestParam String code, @RequestParam String retailerCode,
                                RedirectAttributes redirect) {
        try {
            tracking.start(code, retailerCode);
            redirect.addFlashAttribute("notice", "Price tracking is on for " + retailerCode + ".");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/start-all")
    public String startAll(@RequestParam String code, RedirectAttributes redirect) {
        try {
            tracking.startAll(code);
            redirect.addFlashAttribute("notice", "Tracking all retailers for this product, including future listings.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/stop-all")
    public String stopAll(@RequestParam String code, RedirectAttributes redirect) {
        try {
            tracking.stopAll(code);
            redirect.addFlashAttribute("notice", "Tracking stopped for all retailers. Saved history remains available.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/stop")
    public String stopTracking(@RequestParam long trackingId, RedirectAttributes redirect) {
        try {
            tracking.stop(trackingId);
            redirect.addFlashAttribute("notice", "Tracking stopped. Saved price history remains available.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/history/delete")
    public String deleteHistory(@RequestParam long trackingId, RedirectAttributes redirect) {
        try {
            tracking.deleteHistory(trackingId);
            redirect.addFlashAttribute("notice", "Price history and its alerts were deleted.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
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
                    ? "Target price alert removed." : "Target price alert saved.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
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
                    ? "Price-drop alert enabled." : "Price-drop alert disabled.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/settings")
    public String settings(@RequestParam LocalTime quietStart, @RequestParam LocalTime quietEnd,
                           @RequestParam LocalTime summaryTime, RedirectAttributes redirect) {
        try {
            notificationPolicy.update(quietStart, quietEnd, summaryTime);
            redirect.addFlashAttribute("notice", "Alert timing settings saved.");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("error", "Check the alert timing values and try again.");
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/events/{eventId}/read")
    public String read(@PathVariable long eventId, RedirectAttributes redirect) {
        try {
            alerts.markRead(eventId);
            redirect.addFlashAttribute("notice", "Alert marked as read.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/events/{eventId}/snooze")
    public String snooze(@PathVariable long eventId, RedirectAttributes redirect) {
        try {
            alerts.snooze(eventId);
            redirect.addFlashAttribute("notice", "Alert snoozed for 24 hours.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/alerts";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badInput(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("error", "Check the price, time, or tracking selection and try again.");
        return screen(model, "error", "Check your input");
    }

    private static String screen(Model model, String scene, String title) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", title);
        return "market-desk";
    }
}
