package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.i18n.AppMessages;
import com.pricewatch.reminder.ReminderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReminderController {
    private final ReminderService reminders;
    private final CatalogService catalog;
    private final HelloController formatter;

    public ReminderController(ReminderService reminders, CatalogService catalog, HelloController formatter) {
        this.reminders = reminders;
        this.catalog = catalog;
        this.formatter = formatter;
    }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", formatter);
    }

    @GetMapping("/reminders")
    String view(@RequestParam(defaultValue = "") String code, Model model) {
        ProductView candidate = null;
        if (!code.isBlank()) {
            try {
                candidate = reminders.candidate(code);
            } catch (IllegalArgumentException exception) {
                model.addAttribute("error", exception.getMessage());
            }
        }
        return show(model, candidate);
    }

    @PostMapping("/reminders/create")
    String create(@RequestParam String code, @RequestParam int quantity, @RequestParam int intervalDays,
                  @RequestParam(defaultValue = "") String retailerCode, RedirectAttributes redirect) {
        try {
            reminders.create(code, quantity, intervalDays, retailerCode);
            redirect.addFlashAttribute("notice", AppMessages.text("notice.reminderSaved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            redirect.addAttribute("code", code);
            return "redirect:/reminders";
        }
        return "redirect:/reminders";
    }

    @PostMapping("/reminders/{id}/clear")
    String clear(@PathVariable long id, RedirectAttributes redirect) {
        return act(id, "notice.reminderCleared", redirect, () -> reminders.clear(id));
    }

    @PostMapping("/reminders/{id}/snooze")
    String snooze(@PathVariable long id, RedirectAttributes redirect) {
        return act(id, "notice.reminderSnoozed", redirect, () -> reminders.snooze(id));
    }

    @PostMapping("/reminders/{id}/retailer")
    String changeRetailer(@PathVariable long id, @RequestParam(defaultValue = "") String retailerCode,
                          RedirectAttributes redirect) {
        return act(id, "notice.reminderRetailer", redirect,
                () -> reminders.changeRetailer(id, retailerCode));
    }

    @PostMapping("/reminders/{id}/add")
    String addToList(@PathVariable long id, RedirectAttributes redirect) {
        return act(id, "notice.reminderAdded", redirect, () -> reminders.addToList(id));
    }

    @PostMapping("/reminders/{id}/delete")
    String delete(@PathVariable long id, RedirectAttributes redirect) {
        return act(id, "notice.reminderDeleted", redirect, () -> reminders.delete(id));
    }

    private String act(long id, String successKey, RedirectAttributes redirect, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("notice", AppMessages.text(successKey));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/reminders";
    }

    private String show(Model model, ProductView candidate) {
        model.addAttribute("overview", reminders.overview());
        model.addAttribute("candidate", candidate);
        model.addAttribute("scene", "reminders");
        model.addAttribute("title", AppMessages.text("page.reminders"));
        return "market-desk";
    }
}
