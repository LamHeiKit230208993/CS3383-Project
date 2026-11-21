package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.preferences.DisplayPreferencesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class DisplayPreferencesController {
    private final DisplayPreferencesService preferences;
    private final CatalogService catalogue;
    private final HelloController format;

    DisplayPreferencesController(DisplayPreferencesService preferences, CatalogService catalogue, HelloController format) {
        this.preferences = preferences;
        this.catalogue = catalogue;
        this.format = format;
    }

    @GetMapping("/preferences")
    String show(Model model) {
        model.addAttribute("scene", "preferences");
        model.addAttribute("title", "Display settings");
        model.addAttribute("importStatus", catalogue.status());
        model.addAttribute("format", format);
        return "market-desk";
    }

    @PostMapping("/preferences")
    String save(@RequestParam(defaultValue = "") String language,
                @RequestParam(defaultValue = "false") boolean highContrast,
                @RequestParam(defaultValue = "false") boolean fromSettings, RedirectAttributes redirect) {
        try {
            preferences.update(language, highContrast);
            redirect.addFlashAttribute("displayFeedback", "preferences.saved");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("displayError", "preferences.unsupportedLanguage");
        }
        return fromSettings ? "redirect:/settings#display" : "redirect:/preferences";
    }
}
