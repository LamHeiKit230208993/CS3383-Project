package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.i18n.AppMessages;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.NotificationPolicy;
import com.pricewatch.tracking.ProductHistoryService;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Controller
public class TrackingController {
    private final CatalogService catalog;
    private final TrackingService tracking;
    private final ProductHistoryService productHistory;
    private final AlertService alerts;
    private final NotificationPolicy notificationPolicy;
    private final HelloController format;
    private final Clock clock;

    public TrackingController(CatalogService catalog, TrackingService tracking,
                              ProductHistoryService productHistory, AlertService alerts,
                              NotificationPolicy notificationPolicy, HelloController format, Clock appClock) {
        this.catalog = catalog;
        this.tracking = tracking;
        this.productHistory = productHistory;
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
        var periods = tracking.trackingPeriods();
        Map<String, String> trackedProducts = new LinkedHashMap<>();
        for (var period : periods) {
            if (period.enabled()) trackedProducts.putIfAbsent(period.productCode(), period.productName());
        }
        model.addAttribute("trackingPeriods", periods);
        model.addAttribute("trackedProducts", trackedProducts);
        model.addAttribute("trackedProductCodes", trackedProducts.keySet());
        if (code != null && !code.isBlank()) {
            try {
                model.addAttribute("productToSave", catalog.find(code));
            } catch (IllegalArgumentException exception) {
                model.addAttribute("productLookupError", exception.getMessage());
            }
        }
        return screen(model, "saved", "page.saved");
    }

    @GetMapping("/history/{code}")
    public String history(@PathVariable String code, Model model) {
        try {
            model.addAttribute("history", productHistory.history(code));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        return screen(model, "history", "page.history");
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        var now = clock.instant();
        alerts.processDue(now);
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("alertEvents", alerts.events(now));
        model.addAttribute("notificationSettings", notificationPolicy.settings());
        model.addAttribute("alertProducts", tracking.alertProducts());
        return screen(model, "alerts", "page.alerts");
    }

    @PostMapping("/saved/save")
    public String save(@RequestParam String code, RedirectAttributes redirect) {
        try {
            tracking.save(code);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.productSaved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/saved/remove")
    public String removeSaved(@RequestParam String code, RedirectAttributes redirect) {
        tracking.removeSaved(code);
        redirect.addFlashAttribute("notice", AppMessages.text("notice.productRemoved"));
        return "redirect:/saved";
    }

    @PostMapping("/tracking/start")
    public String startTracking(@RequestParam String code, @RequestParam String retailerCode,
                                RedirectAttributes redirect) {
        try {
            tracking.start(code, retailerCode);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.trackingStartedRetailer", retailerCode));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/start-all")
    public String startAll(@RequestParam String code, RedirectAttributes redirect) {
        try {
            tracking.startAll(code);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.trackingStartedAll"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/stop-all")
    public String stopAll(@RequestParam String code, RedirectAttributes redirect) {
        try {
            tracking.stopAll(code);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.trackingStoppedAll"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/stop")
    public String stopTracking(@RequestParam long trackingId, RedirectAttributes redirect) {
        try {
            tracking.stop(trackingId);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.trackingStopped"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/tracking/history/delete")
    public String deleteHistory(@RequestParam long trackingId, RedirectAttributes redirect) {
        try {
            tracking.deleteHistory(trackingId);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.historyDeleted"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/saved";
    }

    @PostMapping("/alerts/target")
    public String target(@RequestParam(required = false) String code,
                         @RequestParam(required = false) Long trackingId,
                         @RequestParam(required = false) BigDecimal targetPrice,
                         RedirectAttributes redirect) {
        try {
            if (code != null && !code.isBlank()) {
                tracking.configureTargetForProduct(code, targetPrice);
            } else if (trackingId != null) {
                tracking.configureTarget(trackingId, targetPrice);
            } else {
                throw new IllegalArgumentException(AppMessages.text("error.tracking.productRequired"));
            }
            redirect.addFlashAttribute("notice", AppMessages.text(targetPrice == null
                    ? "notice.targetRemoved" : "notice.targetSaved"));
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
            redirect.addFlashAttribute("notice", AppMessages.text(enabled
                    ? "notice.dropEnabled" : "notice.dropDisabled"));
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
            redirect.addFlashAttribute("notice", AppMessages.text("notice.timingSaved"));
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("error", AppMessages.text("notice.timingInvalid"));
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/events/{eventId}/read")
    public String read(@PathVariable long eventId, RedirectAttributes redirect) {
        try {
            alerts.markRead(eventId);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.alertRead"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/events/{eventId}/snooze")
    public String snooze(@PathVariable long eventId, RedirectAttributes redirect) {
        try {
            alerts.snooze(eventId);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.alertSnoozed"));
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
        model.addAttribute("error", AppMessages.text("error.pricing"));
        return screen(model, "error", "page.error");
    }

    private static String screen(Model model, String scene, String title) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", AppMessages.text(title));
        return "market-desk";
    }
}
