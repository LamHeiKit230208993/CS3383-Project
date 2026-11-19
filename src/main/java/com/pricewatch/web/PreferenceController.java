package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.i18n.AppMessages;
import com.pricewatch.preferences.PreferenceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PreferenceController {
    private final PreferenceService preferences;
    private final CatalogService catalog;
    private final HelloController format;

    public PreferenceController(PreferenceService preferences, CatalogService catalog, HelloController format) {
        this.preferences = preferences;
        this.catalog = catalog;
        this.format = format;
    }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
    }

    @GetMapping("/preferences")
    String view(Model model) {
        model.addAttribute("scene", "preferences");
        model.addAttribute("title", AppMessages.text("page.preferences"));
        return "market-desk";
    }

    @PostMapping("/preferences")
    String save(@RequestParam(defaultValue = "") String locale,
                @RequestParam(defaultValue = "") String contrast,
                RedirectAttributes redirect) {
        try {
            preferences.update(locale, contrast);
            redirect.addFlashAttribute("notice", AppMessages.text("preferences.saved"));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/preferences";
    }
}
