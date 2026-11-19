package com.pricewatch.preferences;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PreferenceModelAdvice {
    private final PreferenceService preferences;

    public PreferenceModelAdvice(PreferenceService preferences) {
        this.preferences = preferences;
    }

    @ModelAttribute
    void preferences(Model model) {
        var settings = preferences.settings();
        model.addAttribute("preferences", settings);
        model.addAttribute("htmlLang", settings.htmlLang());
        model.addAttribute("contrast", settings.highContrast() ? PreferenceService.HIGH_CONTRAST
                : PreferenceService.NORMAL_CONTRAST);
    }
}
