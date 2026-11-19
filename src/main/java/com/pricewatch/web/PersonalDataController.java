package com.pricewatch.web;

import com.pricewatch.budget.BudgetService;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.personal.PersonalDataExport;
import com.pricewatch.personal.PersonalDataService;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

import static com.pricewatch.web.MilestoneMessages.inputError;
import static com.pricewatch.web.MilestoneMessages.text;

@Controller
class PersonalDataController {
    private final PersonalDataService personalData;
    private final CatalogService catalogue;
    private final HelloController format;
    private final BudgetService budgets;
    private final MessageSource messages;

    PersonalDataController(PersonalDataService personalData, CatalogService catalogue, HelloController format,
                           BudgetService budgets, MessageSource messages) {
        this.personalData = personalData;
        this.catalogue = catalogue;
        this.format = format;
        this.budgets = budgets;
        this.messages = messages;
    }

    @GetMapping("/personal-data")
    String show(HttpServletRequest request, HttpServletResponse response, Model model) {
        return overview(request, response, model);
    }

    @GetMapping("/budget")
    String legacyBudget() {
        return "redirect:/personal-data#budget";
    }

    @PostMapping({"/personal-data/budget", "/budget"})
    String saveBudget(@RequestParam(defaultValue = "") String period,
                      @RequestParam(name = "limit", defaultValue = "") String limit,
                      HttpServletRequest request, HttpServletResponse response, Model model,
                      RedirectAttributes redirect) {
        try {
            var savedPeriod = budgets.save(period, limit);
            redirect.addFlashAttribute("budgetNotice", text(messages, "budget.saved",
                    text(messages, "budget.period." + savedPeriod.name())));
            return "redirect:/personal-data#budget";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("budgetError", inputError(messages, exception));
            model.addAttribute("budgetFormPeriod", period);
            model.addAttribute("budgetFormLimit", limit);
            return overview(request, response, model);
        }
    }

    private String overview(HttpServletRequest request, HttpServletResponse response, Model model) {
        var budget = budgets.view();
        model.addAttribute("overview", personalData.overview());
        model.addAttribute("chatCount", AskController.history(request.getSession(false)).size());
        model.addAttribute("budget", budget);
        if (!model.containsAttribute("budgetFormPeriod")) {
            model.addAttribute("budgetFormPeriod", budget.period().name());
            model.addAttribute("budgetFormLimit", budget.limitAmount() == null ? "" : budget.limitAmount().toPlainString());
        }
        return screen(response, model, false);
    }

    @GetMapping(value = "/personal-data/export", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PersonalDataExport> export(HttpServletRequest request, Locale locale) {
        var chats = AskController.history(request.getSession(false)).stream()
                .map(chat -> new PersonalDataExport.Chat(chat.id(), chat.title(), chat.productCode(),
                        "", chat.candidates().stream()
                        .map(choice -> new PersonalDataExport.Choice(choice.code(), choice.label())).toList(),
                        chat.messages().stream().map(PersonalDataController::exportMessage).toList()))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("market-desk-personal-data.json").build().toString())
                .body(personalData.export(chats, locale));
    }

    private static PersonalDataExport.Message exportMessage(AskController.Message message) {
        var cards = message.priceCards().stream().map(card -> {
            var prices = card.rows().stream()
                    .map(row -> new PersonalDataExport.PriceRow(row.retailer(), row.price(), row.lowest())).toList();
            return new PersonalDataExport.PriceCard(card.productCode(), card.productLabel(), prices, card.retrievedAt());
        }).toList();
        return new PersonalDataExport.Message(message.shopper(), message.text(), cards);
    }

    @GetMapping("/personal-data/reset")
    String confirmReset(HttpServletResponse response, Model model) {
        return screen(response, model, true);
    }

    @PostMapping("/personal-data/reset")
    String reset(@RequestParam(name = "confirm", defaultValue = "false") boolean confirmed,
                 HttpServletRequest request, RedirectAttributes redirect) {
        if (!confirmed) {
            redirect.addFlashAttribute("personalError", "personal.confirmRequired");
            return "redirect:/personal-data/reset";
        }
        try {
            personalData.reset();
        } catch (DataAccessException | TransactionException | PersistenceException failure) {
            LoggerFactory.getLogger(getClass()).warn("Local personal data reset failed ({})",
                    failure.getClass().getSimpleName());
            redirect.addFlashAttribute("personalError", "personal.resetFailed");
            return "redirect:/personal-data/reset";
        }
        // The service has committed before we clear any current session data.
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        redirect.addFlashAttribute("personalFeedback", "personal.resetDone");
        return "redirect:/personal-data";
    }

    private String screen(HttpServletResponse response, Model model, boolean confirmingReset) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        model.addAttribute("scene", "personal");
        model.addAttribute("title", "Personal data");
        model.addAttribute("confirmingReset", confirmingReset);
        model.addAttribute("importStatus", catalogue.status());
        model.addAttribute("format", format);
        return "market-desk";
    }
}
