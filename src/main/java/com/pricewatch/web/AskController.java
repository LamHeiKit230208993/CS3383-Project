package com.pricewatch.web;

import com.pricewatch.ask.AskResult;
import com.pricewatch.ask.AskService;
import com.pricewatch.catalog.CatalogService;
import com.pricewatch.i18n.AppMessages;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AskController {
    private final AskService ask;
    private final CatalogService catalog;
    private final HelloController format;

    public AskController(AskService ask, CatalogService catalog, HelloController format) {
        this.ask = ask;
        this.catalog = catalog;
        this.format = format;
    }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
    }

    @GetMapping("/ask")
    String view(Model model) {
        return show(model, "", null);
    }

    @PostMapping("/ask")
    String submit(@RequestParam(defaultValue = "") String question,
                  @RequestParam(defaultValue = "") String productCode,
                  Model model) {
        AskResult result = null;
        try {
            result = ask.ask(question, productCode);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        }
        return show(model, question, result);
    }

    private String show(Model model, String question, AskResult result) {
        model.addAttribute("scene", "ask");
        model.addAttribute("title", AppMessages.text("page.ask"));
        model.addAttribute("question", question);
        model.addAttribute("result", result);
        return "market-desk";
    }
}
