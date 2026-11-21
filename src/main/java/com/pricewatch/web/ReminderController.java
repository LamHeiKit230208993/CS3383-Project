package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
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
    private final UiMessages messages;

    public ReminderController(ReminderService reminders, CatalogService catalog, HelloController formatter,
                              UiMessages messages) {
        this.reminders = reminders;
        this.catalog = catalog;
        this.formatter = formatter;
        this.messages = messages;
    }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", formatter);
    }

    @GetMapping("/reminders")
    String view(@RequestParam(defaultValue = "") String code, Model model,
                jakarta.servlet.http.HttpServletRequest request) {
        ProductView candidate = null;
        if (!code.isBlank()) {
            try {
                candidate = reminders.candidate(code);
            } catch (IllegalArgumentException exception) {
                model.addAttribute("error", messages.error(request, exception));
            }
        }
        return show(model, request, candidate);
    }

    @PostMapping("/reminders/create")
    String create(@RequestParam String code, @RequestParam int quantity, @RequestParam int intervalDays,
                  @RequestParam(defaultValue = "") String retailerCode, RedirectAttributes redirect,
                  jakarta.servlet.http.HttpServletRequest request) {
        try {
            reminders.create(code, quantity, intervalDays, retailerCode);
            redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.reminderSaved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
            redirect.addAttribute("code", code);
            return "redirect:/reminders";
        }
        return "redirect:/reminders";
    }

    @PostMapping("/reminders/{id}/clear")
    String clear(@PathVariable long id, RedirectAttributes redirect,
                      jakarta.servlet.http.HttpServletRequest request) {
        return act(id, "ui.notice.reminderCleared", redirect, request, () -> reminders.clear(id));
    }

    @PostMapping("/reminders/{id}/snooze")
    String snooze(@PathVariable long id, RedirectAttributes redirect,
                      jakarta.servlet.http.HttpServletRequest request) {
        return act(id, "ui.notice.reminderSnoozed", redirect, request, () -> reminders.snooze(id));
    }

    @PostMapping("/reminders/{id}/add")
    String addToList(@PathVariable long id, RedirectAttributes redirect,
                      jakarta.servlet.http.HttpServletRequest request) {
        return act(id, "ui.notice.reminderAdded", redirect, request, () -> reminders.addToList(id));
    }

    @PostMapping("/reminders/{id}/delete")
    String delete(@PathVariable long id, RedirectAttributes redirect,
                      jakarta.servlet.http.HttpServletRequest request) {
        return act(id, "ui.notice.reminderDeleted", redirect, request, () -> reminders.delete(id));
    }

    private String act(long id, String successKey, RedirectAttributes redirect,
                       jakarta.servlet.http.HttpServletRequest request, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("notice", messages.get(request, successKey));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", messages.error(request, exception));
        }
        return "redirect:/reminders";
    }

    private String show(Model model, jakarta.servlet.http.HttpServletRequest request,
                        ProductView candidate) {
        model.addAttribute("overview", reminders.overview());
        model.addAttribute("candidate", candidate);
        model.addAttribute("scene", "reminders");
        model.addAttribute("title", messages.get(request, "ui.title.reminders"));
        return "market-desk";
    }
}
