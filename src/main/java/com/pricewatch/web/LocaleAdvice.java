package com.pricewatch.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Locale;

/** J12: values every template needs for the language and contrast controls. */
@ControllerAdvice
public class LocaleAdvice {
    @ModelAttribute
    void preferences(Model model, HttpServletRequest request) {
        Locale locale = LocaleController.requestLocale(request);
        org.springframework.context.i18n.LocaleContextHolder.setLocale(locale);
        model.addAttribute("htmlLang", LocaleController.htmlLang(locale));
        model.addAttribute("highContrast", LocaleController.highContrast(request));
        model.addAttribute("currentPath", request.getRequestURI());
        model.addAttribute("activeLanguage", LocaleController.languageCode(locale));
        model.addAttribute("localeChoices", List.of(
                new LocaleChoice("en", "English", "English"),
                new LocaleChoice("zh_TW", "\u7E41\u9AD4\u4E2D\u6587", "Traditional Chinese"),
                new LocaleChoice("zh_CN", "\u7B80\u4F53\u4E2D\u6587", "Simplified Chinese")));
        model.addAttribute("labels", new ProductLabels(locale));
    }

    public record LocaleChoice(String code, String label, String ariaLabel) {
    }

    /** Template helper: ${labels.of(product.name)}. */
    public record ProductLabels(Locale locale) {
        public String of(com.pricewatch.catalog.LocalizedText text) {
            return LocaleController.label(text, locale);
        }
    }
}
