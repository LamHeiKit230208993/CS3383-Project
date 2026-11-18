package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.reminder.ReminderView;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.pricewatch.web.MilestoneMessages.inputError;
import static com.pricewatch.web.MilestoneMessages.text;

@Controller
public class ReminderController {
    private final ReminderService reminders;
    private final CatalogService catalog;
    private final HelloController formatter;
    private final MessageSource messages;

    public ReminderController(ReminderService reminders, CatalogService catalog, HelloController formatter,
                              MessageSource messages) {
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
    String view(@RequestParam(defaultValue = "") String code, Model model) {
        ProductView candidate = null;
        if (!code.isBlank()) {
            try {
                candidate = reminders.candidate(code);
            } catch (IllegalArgumentException exception) {
                model.addAttribute("error", inputError(messages, exception));
            }
        }
        return show(model, candidate);
    }

    @PostMapping("/reminders/create")
    String create(@RequestParam String code, @RequestParam int quantity, @RequestParam int intervalDays,
                  @RequestParam(defaultValue = "") String retailerCode, RedirectAttributes redirect) {
        try {
            reminders.create(code, quantity, intervalDays, retailerCode);
            redirect.addFlashAttribute("notice", text(messages, "reminders.notice.saved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
            redirect.addAttribute("code", code);
            return "redirect:/reminders";
        }
        return "redirect:/reminders";
    }

    @PostMapping("/reminders/{id}/clear")
    String clear(@PathVariable long id, RedirectAttributes redirect) {
        return act("reminders.notice.cleared", redirect,
                () -> reminders.clear(id));
    }

    @PostMapping("/reminders/{id}/snooze")
    String snooze(@PathVariable long id, RedirectAttributes redirect) {
        return act("reminders.notice.snoozed", redirect,
                () -> reminders.snooze(id));
    }

    @PostMapping("/reminders/{id}/add")
    String addToList(@PathVariable long id, RedirectAttributes redirect) {
        return act("reminders.notice.added", redirect,
                () -> reminders.addToList(id));
    }

    @PostMapping("/reminders/{id}/delete")
    String delete(@PathVariable long id, RedirectAttributes redirect) {
        return act("reminders.notice.deleted", redirect, () -> reminders.delete(id));
    }

    private String act(String successKey, RedirectAttributes redirect, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("notice", text(messages, successKey));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
        return "redirect:/reminders";
    }

    private String show(Model model, ProductView candidate) {
        var overview = reminders.overview();
        model.addAttribute("overview", overview);
        model.addAttribute("productLabels", catalog.localizedLabels(overview.reminders().stream()
                .map(ReminderView::productCode).toList(), LocaleContextHolder.getLocale()));
        model.addAttribute("candidate", candidate);
        model.addAttribute("scene", "reminders");
        model.addAttribute("title", text(messages, "reminders.title"));
        return "market-desk";
    }
}
