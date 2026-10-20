package com.pricewatch.web;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Serves the static UI prototype. There is no database, import, REST API, AI,
 * or notification service behind these pages; the model only carries sample data.
 */
@Controller
public class PrototypeController {

    private static final BigDecimal BUDGET = new BigDecimal("600.00");

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("sourceLabel", "Consumer Council Online Price Watch · sample");
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/search";
    }

    @GetMapping("/search")
    public String search(Model model) {
        model.addAttribute("tag", "Search prices");
        model.addAttribute("subtitle", "Compare listed prices across retailers. All data is sample.");
        model.addAttribute("active", "search");
        model.addAttribute("products", SampleData.PRODUCTS);
        return "search";
    }

    @GetMapping("/compare")
    public String compare(@RequestParam(name = "product", required = false) String productCode, Model model) {
        SampleData.Product product = SampleData.PRODUCTS.stream()
                .filter(item -> item.code().equals(productCode))
                .findFirst()
                .orElse(SampleData.MILK);

        BigDecimal best = new BigDecimal(product.bestPrice());
        BigDecimal next = product.listings().stream()
                .filter(listing -> !"missing".equals(listing.state()))
                .map(listing -> new BigDecimal(listing.price()))
                .filter(price -> price.compareTo(best) > 0)
                .min(BigDecimal::compareTo)
                .orElse(best);
        BigDecimal saving = next.subtract(best);

        model.addAttribute("tag", "Compare retailers");
        model.addAttribute("subtitle", "One exact product and pack, compared across sample retailer listings.");
        model.addAttribute("active", "search");
        model.addAttribute("product", product);
        model.addAttribute("listings", product.listings());
        model.addAttribute("savingNote", "$" + saving.setScale(2, RoundingMode.HALF_UP).toPlainString()
                + " below the next cheapest sample listing");
        return "compare";
    }

    @GetMapping("/list")
    public String list(Model model) {
        BigDecimal total = SampleData.LIST_ITEMS.stream()
                .map(item -> new BigDecimal(item.price()).multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int percent = total.multiply(BigDecimal.valueOf(100))
                .divide(BUDGET, 0, RoundingMode.HALF_UP)
                .intValue();

        model.addAttribute("tag", "Shopping list");
        model.addAttribute("subtitle", "Estimated basket cost against a planned weekly budget.");
        model.addAttribute("active", "list");
        model.addAttribute("listItems", SampleData.LIST_ITEMS);
        model.addAttribute("estimatedTotal", total.setScale(2, RoundingMode.HALF_UP).toPlainString());
        model.addAttribute("budget", BUDGET.toPlainString());
        model.addAttribute("budgetPercent", percent);
        return "list";
    }

    @GetMapping("/saved")
    public String saved(Model model) {
        model.addAttribute("tag", "Saved items");
        model.addAttribute("subtitle", "A wishlist is not tracking. Tracking stores sample price history.");
        model.addAttribute("active", "saved");
        model.addAttribute("wishlist", SampleData.WISHLIST);
        model.addAttribute("tracking", SampleData.TRACKING);
        return "saved";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("tag", "Alerts & reminders");
        model.addAttribute("subtitle", "Target prices, price drops, quiet hours, and reminders. In-app only.");
        model.addAttribute("active", "alerts");
        model.addAttribute("alerts", SampleData.ALERTS);
        return "alerts";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("tag", "Price history");
        model.addAttribute("subtitle", "Only the dates available are shown. History is not a prediction.");
        model.addAttribute("active", "history");
        model.addAttribute("product", SampleData.MILK);
        model.addAttribute("historyPoints", SampleData.HISTORY);
        return "history";
    }

    @GetMapping("/assistant")
    public String assistant(Model model) {
        model.addAttribute("tag", "Price assistant");
        model.addAttribute("subtitle", "A sample answer built from a checked sample comparison.");
        model.addAttribute("active", "assistant");
        return "assistant";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("tag", "Settings");
        model.addAttribute("subtitle", "Language preview, high-contrast display, and prototype limits.");
        model.addAttribute("active", "settings");
        return "settings";
    }
}
