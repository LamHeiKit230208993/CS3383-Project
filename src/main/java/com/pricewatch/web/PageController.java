package com.pricewatch.web;

import com.pricewatch.domain.Catalogue;
import com.pricewatch.domain.Comparison;
import com.pricewatch.domain.ListItem;
import com.pricewatch.domain.Listing;
import com.pricewatch.domain.LocalisedName;
import com.pricewatch.domain.Product;
import com.pricewatch.domain.ShoppingList;
import com.pricewatch.importing.CatalogueImporter;
import com.pricewatch.importing.ImportResult;
import com.pricewatch.store.CatalogueStore;
import com.pricewatch.store.ShoppingListStore;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class PageController {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final CatalogueStore catalogueStore;
    private final ShoppingListStore listStore;
    private final CatalogueImporter importer;

    public PageController(CatalogueStore catalogueStore, ShoppingListStore listStore, CatalogueImporter importer) {
        this.catalogueStore = catalogueStore;
        this.listStore = listStore;
        this.importer = importer;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "q", required = false) String query,
                       @RequestParam(name = "lang", required = false, defaultValue = "en") String lang,
                       Model model) {
        Catalogue catalogue = catalogueStore.get();
        List<Product> results = catalogue == null ? List.of() : catalogue.find(query);
        if (catalogue != null && (query == null || query.isBlank())) {
            results = catalogue.getProducts();
        }
        List<ProductView> views = new ArrayList<>();
        for (Product product : results) {
            Comparison comparison = product.compare(catalogue.getImportedAt());
            views.add(new ProductView(product, comparison, lang));
        }
        model.addAttribute("productViews", views);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("resultCount", views.size() + (views.size() == 1 ? " product" : " products"));
        model.addAttribute("importNote", importNote(catalogue));
        common(model, lang);
        return "market-desk";
    }

    @GetMapping("/list")
    public String list(@RequestParam(name = "lang", required = false, defaultValue = "en") String lang,
                       Model model) {
        ShoppingList list = listStore.load();
        Catalogue catalogue = catalogueStore.get();
        List<ListRow> rows = new ArrayList<>();
        for (ListItem item : list.getItems()) {
            Product product = catalogue == null ? null : catalogue.product(item.getProductCode());
            if (product == null) {
                rows.add(ListRow.unavailable(item));
                continue;
            }
            rows.add(new ListRow(item, product, product.compare(catalogue.getImportedAt()), lang));
        }
        model.addAttribute("listRows", rows);
        model.addAttribute("listName", list.getName());
        model.addAttribute("listProgress", list.getItems().size() + " items \u00b7 " + list.checkedCount() + " checked");
        common(model, lang);
        return "list";
    }

    @GetMapping("/compare/{code}")
    public String compare(@PathVariable String code,
                          @RequestParam(name = "lang", required = false, defaultValue = "en") String lang,
                          Model model) {
        Catalogue catalogue = catalogueStore.get();
        Product product = catalogue == null ? null : catalogue.product(code);
        if (product == null) {
            return "redirect:/";
        }
        Comparison comparison = product.compare(catalogue.getImportedAt());
        List<ListingRow> rows = new ArrayList<>();
        for (Listing listing : comparison.getCheapest()) {
            rows.add(new ListingRow(listing, true, lang));
        }
        for (Listing listing : comparison.getPriced()) {
            if (!comparison.getCheapest().contains(listing)) {
                rows.add(new ListingRow(listing, false, lang));
            }
        }
        for (String retailer : comparison.getMissing()) {
            rows.add(ListingRow.missing(retailer));
        }
        model.addAttribute("comparison", comparison);
        model.addAttribute("rows", rows);
        model.addAttribute("productName", product.getName().text(lang));
        model.addAttribute("productBrand", product.getBrand().text(lang));
        model.addAttribute("productCategory", product.getCategory1().text(lang));
        model.addAttribute("bestTag", !comparison.hasWinner() ? "No listed price"
                : comparison.isTie() ? "Tied prices" : "Lowest listed");
        model.addAttribute("retrievedAt", WHEN.format(comparison.getImportedAt()));
        common(model, lang);
        return "compare";
    }

    @PostMapping("/import/sample")
    public String importSample(RedirectAttributes redirect) {
        redirect.addFlashAttribute("importMessage", describe(importer.importSample()));
        return "redirect:/";
    }

    @PostMapping("/import/daily")
    public String importDaily(RedirectAttributes redirect) {
        redirect.addFlashAttribute("importMessage", describe(importer.importDaily()));
        return "redirect:/";
    }

    @PostMapping("/list/items")
    public String addItem(@RequestParam String code,
                          @RequestParam(defaultValue = "1") int quantity,
                          RedirectAttributes redirect) {
        ShoppingList list = listStore.load();
        if (quantity < 1) {
            redirect.addFlashAttribute("listMessage", "Choose a quantity of at least 1. Your list was not changed.");
            return "redirect:/list";
        }
        if (catalogueStore.get() == null || catalogueStore.get().product(code) == null) {
            redirect.addFlashAttribute("listMessage", "That product is not in the stored catalogue. Your list was not changed.");
            return "redirect:/";
        }
        list.add(code, quantity, Instant.now());
        listStore.save(list);
        return "redirect:/list";
    }

    @PostMapping("/list/items/{code}/quantity")
    public String changeQuantity(@PathVariable String code, @RequestParam int quantity, RedirectAttributes redirect) {
        ShoppingList list = listStore.load();
        try {
            list.changeQuantity(code, quantity);
            listStore.save(list);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("listMessage", e.getMessage() + ". Your list was not changed.");
        }
        return "redirect:/list";
    }

    @PostMapping("/list/items/{code}/toggle")
    public String toggle(@PathVariable String code,
                         @RequestParam(required = false) String checked,
                         RedirectAttributes redirect) {
        ShoppingList list = listStore.load();
        try {
            list.toggle(code, "true".equals(checked));
            listStore.save(list);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("listMessage", e.getMessage() + ". Your list was not changed.");
        }
        return "redirect:/list";
    }

    @PostMapping("/list/items/{code}/remove")
    public String remove(@PathVariable String code, RedirectAttributes redirect) {
        ShoppingList list = listStore.load();
        try {
            list.remove(code);
            listStore.save(list);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("listMessage", e.getMessage() + ". Your list was not changed.");
        }
        return "redirect:/list";
    }

    private void common(Model model, String lang) {
        ShoppingList list = listStore.load();
        model.addAttribute("lang", lang);
        model.addAttribute("navCount", list.getItems().size());
    }

    private String importNote(Catalogue catalogue) {
        if (catalogue == null) {
            return "No catalogue imported yet. Import the sample file to start.";
        }
        String source = catalogue.getSourceDate() == null
                ? "source date unknown"
                : "source date " + catalogue.getSourceDate();
        return "Retrieved " + WHEN.format(catalogue.getImportedAt()) + " \u00b7 " + source;
    }

    private String describe(ImportResult result) {
        if (result.accepted()) {
            return "Import accepted: " + result.reason() + ". The previous catalogue was replaced.";
        }
        return "Import rejected: " + result.reason() + ". The last accepted catalogue was kept.";
    }

    public record ProductView(String code, String brand, String name, String category,
                              String lowestPrice, String lowestRetailer) {
        ProductView(Product product, Comparison comparison, String lang) {
            this(product.getCode(), product.getBrand().text(lang), product.getName().text(lang),
                    product.getCategory1().text(lang),
                    comparison.hasWinner() ? Money.hkd(comparison.getCheapest().get(0).getPrice(), lang) : "No listed price",
                    comparison.hasWinner() ? comparison.getCheapest().get(0).getRetailerCode() : "\u2014");
        }
    }

    public record ListingRow(String retailer, String offer, String price, boolean lowest, boolean missing) {
        ListingRow(Listing listing, boolean lowest, String lang) {
            this(listing.getRetailerCode(), listing.getOffer().text(lang),
                    listing.hasPrice() ? Money.hkd(listing.getPrice(), lang) : null, lowest, false);
        }

        static ListingRow missing(String retailer) {
            return new ListingRow(retailer, "", null, false, true);
        }
    }

    public record ListRow(String code, String name, String detail, int quantity, boolean checked,
                          String lowestPrice, String lowestRetailer, String subtotal, boolean unavailable) {
        ListRow(ListItem item, Product product, Comparison comparison, String lang) {
            this(product.getCode(), product.getName().text(lang),
                    product.getCategory1().text(lang) + " \u00b7 " + item.getQuantity()
                            + (item.getQuantity() == 1 ? " pack" : " packs"),
                    item.getQuantity(), item.isChecked(),
                    comparison.hasWinner() ? Money.hkd(comparison.getCheapest().get(0).getPrice(), lang) : "No listed price",
                    comparison.hasWinner() ? comparison.getCheapest().get(0).getRetailerCode() : "\u2014",
                    comparison.hasWinner()
                            ? Money.hkd(comparison.getCheapest().get(0).getPrice()
                                    .multiply(java.math.BigDecimal.valueOf(item.getQuantity())), lang)
                            : null,
                    false);
        }

        static ListRow unavailable(ListItem item) {
            return new ListRow(item.getProductCode(), item.getProductCode(),
                    "No longer in the stored catalogue \u00b7 " + item.getQuantity()
                            + (item.getQuantity() == 1 ? " pack" : " packs"),
                    item.getQuantity(), item.isChecked(),
                    "Unavailable", "\u2014", null, true);
        }
    }
}
