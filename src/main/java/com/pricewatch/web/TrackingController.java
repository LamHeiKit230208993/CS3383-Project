package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.tracking.TrackingService;
import org.springframework.context.MessageSource;
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
import java.util.List;

import static com.pricewatch.web.MilestoneMessages.inputError;
import static com.pricewatch.web.MilestoneMessages.text;

@Controller
public class TrackingController {
    private final CatalogService catalog;
    private final TrackingService tracking;
    private final AlertService alerts;
    private final ShoppingService shopping;
    private final HelloController format;
    private final MessageSource messages;

    public TrackingController(CatalogService catalog, TrackingService tracking, AlertService alerts,
                              HelloController format,
                              MessageSource messages, ShoppingService shopping) {
        this.catalog = catalog;
        this.tracking = tracking;
        this.alerts = alerts;
        this.format = format;
        this.messages = messages;
        this.shopping = shopping;
    }

    @GetMapping("/saved")
    public String saved(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        var products = tracking.savedProducts();
        model.addAttribute("savedProducts", products);
        model.addAttribute("openCode", code);
        if (code != null && !code.isBlank() && products.stream().noneMatch(saved -> saved.product().code().equals(code))) {
            try {
                model.addAttribute("productToSave", catalog.find(code));
            } catch (IllegalArgumentException exception) {
                model.addAttribute("productLookupError", text(messages, "milestone.error.productMissing"));
            }
        }
        return screen(model, "saved", "Wishlist");
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

    @PostMapping("/saved/cart")
    public String addToCart(@RequestParam(name = "codes", required = false) List<String> codes,
                            RedirectAttributes redirect) {
        if (codes == null || codes.isEmpty()) {
            redirect.addFlashAttribute("error", text(messages, "wishlist.choose"));
        } else {
            var result = shopping.addMissingProducts(codes);
            redirect.addFlashAttribute("notice", text(messages, "wishlist.added",
                    result.added(), result.existing(), result.unavailable()));
        }
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
                         @RequestParam(required = false) String code,
                         RedirectAttributes redirect) {
        try {
            tracking.configureTarget(trackingId, targetPrice);
            redirect.addFlashAttribute("notice", targetPrice == null
                    ? text(messages, "alerts.notice.targetRemoved") : text(messages, "alerts.notice.targetSaved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return ruleDestination(code, redirect);
    }

    @PostMapping("/alerts/drop")
    public String drop(@RequestParam long trackingId,
                       @RequestParam(defaultValue = "false") boolean enabled,
                       @RequestParam(required = false) String code,
                       RedirectAttributes redirect) {
        try {
            tracking.configureDrop(trackingId, enabled);
            redirect.addFlashAttribute("notice", enabled
                    ? text(messages, "alerts.notice.dropEnabled") : text(messages, "alerts.notice.dropDisabled"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return ruleDestination(code, redirect);
    }

    @PostMapping("/alerts/settings")
    public String retiredSettings() {
        return "redirect:/alerts";
    }

    private static String ruleDestination(String code, RedirectAttributes redirect) {
        if (code == null) return "redirect:/alerts";
        redirect.addAttribute("code", code);
        return "redirect:/alerts/rules";
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

    private static String screen(Model model, String scene, String title) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", title);
        return "market-desk";
    }
}
