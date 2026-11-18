package com.pricewatch.web;

import com.pricewatch.budget.BudgetService;
import com.pricewatch.catalog.CatalogService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.pricewatch.web.MilestoneMessages.inputError;
import static com.pricewatch.web.MilestoneMessages.text;

@Controller
public class BudgetController {
    private final BudgetService budgets;
    private final CatalogService catalog;
    private final HelloController formatter;
    private final MessageSource messages;

    public BudgetController(BudgetService budgets, CatalogService catalog, HelloController formatter,
                            MessageSource messages) {
        this.budgets = budgets;
        this.catalog = catalog;
        this.formatter = formatter;
        this.messages = messages;
    }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", formatter);
    }

    @GetMapping("/budget")
    String view(Model model) {
        return show(model, null, null);
    }

    @PostMapping("/budget")
    String save(@RequestParam(defaultValue = "") String period,
                @RequestParam(name = "limit", defaultValue = "") String limit,
                Model model,
                RedirectAttributes redirect) {
        try {
            var savedPeriod = budgets.save(period, limit);
            redirect.addFlashAttribute("notice", text(messages, "budget.saved", text(messages, "budget.period." + savedPeriod.name())));
            return "redirect:/budget";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", inputError(messages, exception));
            return show(model, period, limit);
        }
    }

    private String show(Model model, String periodInput, String limitInput) {
        var budget = budgets.view();
        model.addAttribute("budget", budget);
        model.addAttribute("budgetFormPeriod", periodInput == null ? budget.period().name() : periodInput);
        model.addAttribute("budgetFormLimit", limitInput == null
                ? budget.limitAmount() == null ? "" : budget.limitAmount().toPlainString()
                : limitInput);
        model.addAttribute("scene", "budget");
        model.addAttribute("title", text(messages, "budget.title"));
        return "market-desk";
    }
}
