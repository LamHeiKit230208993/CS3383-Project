package com.pricewatch.web;

import com.pricewatch.budget.BudgetService;
import com.pricewatch.catalog.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BudgetController {
    private final BudgetService budgets;
    private final CatalogService catalog;
    private final HelloController formatter;
    private final UiMessages messages;

    public BudgetController(BudgetService budgets, CatalogService catalog, HelloController formatter,
                            UiMessages messages) {
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
    String view(Model model, jakarta.servlet.http.HttpServletRequest request) {
        return show(model, request, null, null);
    }

    @PostMapping("/budget")
    String save(@RequestParam(defaultValue = "") String period,
                @RequestParam(name = "limit", defaultValue = "") String limit,
                Model model,
                RedirectAttributes redirect,
                jakarta.servlet.http.HttpServletRequest request) {
        try {
            var savedPeriod = budgets.save(period, limit);
            redirect.addFlashAttribute("notice",
                    messages.get(request, "ui.notice.budgetSaved",
                            messages.get(request, savedPeriod == com.pricewatch.budget.BudgetPeriod.MONTHLY
                                    ? "budget.monthly" : "budget.weekly")));
            return "redirect:/budget";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", messages.error(request, exception));
            return show(model, request, period, limit);
        }
    }

    private String show(Model model, jakarta.servlet.http.HttpServletRequest request,
                        String periodInput, String limitInput) {
        var budget = budgets.view();
        model.addAttribute("budget", budget);
        model.addAttribute("budgetFormPeriod", periodInput == null ? budget.period().name() : periodInput);
        model.addAttribute("budgetFormLimit", limitInput == null
                ? budget.limitAmount() == null ? "" : budget.limitAmount().toPlainString()
                : limitInput);
        model.addAttribute("scene", "budget");
        model.addAttribute("title", messages.get(request, "ui.title.budget"));
        return "market-desk";
    }
}
