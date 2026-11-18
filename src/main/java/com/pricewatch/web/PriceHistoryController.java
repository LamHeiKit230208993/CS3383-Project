package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.tracking.TrackingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
class PriceHistoryController {
    private final CatalogService catalog;
    private final TrackingService tracking;
    private final HelloController format;

    PriceHistoryController(CatalogService catalog, TrackingService tracking, HelloController format) {
        this.catalog = catalog;
        this.tracking = tracking;
        this.format = format;
    }

    @GetMapping("/products/{code}/history")
    String history(@PathVariable String code, Model model) {
        ProductView product;
        try {
            product = catalog.find(code);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("scene", "history");
        model.addAttribute("title", "Price history");
        model.addAttribute("product", product);
        model.addAttribute("observations", tracking.priceHistory(code));
        model.addAttribute("format", format);
        model.addAttribute("importStatus", catalog.status());
        return "market-desk";
    }
}
