package com.pricewatch.web;

import com.pricewatch.reminder.ReminderService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.pricewatch.web.MilestoneMessages.inputError;
import static com.pricewatch.web.MilestoneMessages.text;

@Controller
public class ReminderController {
    private final ReminderService reminders;
    private final MessageSource messages;

    public ReminderController(ReminderService reminders, MessageSource messages) {
        this.reminders = reminders;
        this.messages = messages;
    }

    @GetMapping("/reminders")
    String view(@RequestParam(defaultValue = "") String code, RedirectAttributes redirect) {
        return redirectToRules(code, redirect);
    }

    @PostMapping("/reminders/create")
    String create(@RequestParam String code, @RequestParam int quantity, @RequestParam int intervalDays,
                  @RequestParam(defaultValue = "") String retailerCode, RedirectAttributes redirect) {
        act("reminders.notice.saved", redirect, () -> reminders.create(code, quantity, intervalDays, retailerCode));
        return redirectToRules(code, redirect);
    }

    @PostMapping("/reminders/{id}/update")
    String update(@PathVariable long id, @RequestParam String code, @RequestParam int quantity,
                  @RequestParam int intervalDays, @RequestParam(defaultValue = "") String retailerCode,
                  RedirectAttributes redirect) {
        act("reminders.notice.saved", redirect, () -> reminders.update(id, quantity, intervalDays, retailerCode));
        return redirectToRules(code, redirect);
    }

    @PostMapping("/reminders/{id}/clear")
    String clear(@PathVariable long id, RedirectAttributes redirect) {
        act("reminders.notice.cleared", redirect, () -> reminders.clear(id));
        return "redirect:/alerts";
    }

    @PostMapping("/reminders/{id}/snooze")
    String snooze(@PathVariable long id, RedirectAttributes redirect) {
        act("reminders.notice.snoozed", redirect, () -> reminders.snooze(id));
        return "redirect:/alerts";
    }

    @PostMapping("/reminders/{id}/add")
    String addToList(@PathVariable long id, RedirectAttributes redirect) {
        act("reminders.notice.added", redirect, () -> reminders.addToList(id));
        return "redirect:/alerts";
    }

    @PostMapping("/reminders/{id}/delete")
    String delete(@PathVariable long id, @RequestParam(defaultValue = "") String code, RedirectAttributes redirect) {
        act("reminders.notice.deleted", redirect, () -> reminders.delete(id));
        return redirectToRules(code, redirect);
    }

    @PostMapping("/alerts/reminders/{id}/clear")
    String clearOccurrence(@PathVariable long id, RedirectAttributes redirect) {
        act("reminders.notice.cleared", redirect, () -> reminders.clearOccurrence(id));
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/reminders/{id}/snooze")
    String snoozeOccurrence(@PathVariable long id, RedirectAttributes redirect) {
        act("reminders.notice.snoozed", redirect, () -> reminders.snoozeOccurrence(id));
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/reminders/{id}/add")
    String addOccurrence(@PathVariable long id, RedirectAttributes redirect) {
        act("reminders.notice.added", redirect, () -> reminders.addOccurrence(id));
        return "redirect:/alerts";
    }

    private void act(String successKey, RedirectAttributes redirect, Runnable action) {
        try {
            action.run();
            redirect.addFlashAttribute("notice", text(messages, successKey));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", inputError(messages, exception));
        }
    }

    private String redirectToRules(String code, RedirectAttributes redirect) {
        if (code.isBlank()) return "redirect:/alerts";
        redirect.addAttribute("code", code);
        return "redirect:/alerts/rules";
    }
}
