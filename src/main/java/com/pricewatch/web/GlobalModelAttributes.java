package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds the shared template contract to every screen. New controllers should rely
 * on these attributes instead of copying import-status or formatting code.
 */
@ControllerAdvice
public class GlobalModelAttributes {
    private final CatalogService catalog;
    private final ViewFormat format;

    public GlobalModelAttributes(CatalogService catalog, ViewFormat format) {
        this.catalog = catalog;
        this.format = format;
    }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
    }
}
