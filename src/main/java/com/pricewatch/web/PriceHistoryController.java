package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.tracking.PriceHistoryEntry;
import com.pricewatch.tracking.TrackingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Controller
class PriceHistoryController {
    private final CatalogService catalog;
    private final TrackingService tracking;
    private final HelloController format;
    private final Clock clock;

    PriceHistoryController(CatalogService catalog, TrackingService tracking, HelloController format, Clock clock) {
        this.catalog = catalog;
        this.tracking = tracking;
        this.format = format;
        this.clock = clock;
    }

    @GetMapping("/products/{code}/history")
    String history(@PathVariable String code, @RequestParam(defaultValue = "90") int days,
                   @RequestParam(defaultValue = "") String retailer, Model model) {
        ProductView product;
        try {
            product = catalog.find(code);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("scene", "history");
        model.addAttribute("title", "Price history");
        model.addAttribute("product", product);
        var retained = tracking.priceHistory(code);
        var retailers = retained.stream().map(PriceHistoryEntry::retailerCode).distinct().sorted().toList();
        String selectedRetailer = retailers.contains(retailer) ? retailer : "";
        int selectedDays = days == 30 ? 30 : 90;
        var cutoff = clock.instant().minus(selectedDays, ChronoUnit.DAYS);
        var observations = retained.stream()
                .filter(entry -> selectedRetailer.isEmpty() || entry.retailerCode().equals(selectedRetailer))
                .filter(entry -> !entry.retrievedAt().isBefore(cutoff))
                .toList();
        model.addAttribute("observations", observations);
        model.addAttribute("retailers", retailers);
        model.addAttribute("selectedRetailer", selectedRetailer);
        model.addAttribute("selectedDays", selectedDays);
        model.addAttribute("historyAvailable", !retained.isEmpty());
        model.addAttribute("chart", PriceHistoryChart.from(observations).orElse(null));
        model.addAttribute("format", format);
        model.addAttribute("importStatus", catalog.status());
        return "market-desk";
    }
}
