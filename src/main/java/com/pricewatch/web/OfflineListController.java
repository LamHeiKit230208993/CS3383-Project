package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.Summary;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
class OfflineListController {
    private final ShoppingService shopping;
    private final CatalogService catalog;
    private final HelloController format;

    OfflineListController(ShoppingService shopping, CatalogService catalog, HelloController format) {
        this.shopping = shopping;
        this.catalog = catalog;
        this.format = format;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @GetMapping(value = "/list/offline", produces = "text/html")
    String download(@RequestParam(required = false) Long listId, HttpServletResponse response, Model model) {
        var lists = shopping.lists();
        var selected = listId == null ? lists.getFirst() : lists.stream()
                .filter(choice -> listId.equals(choice.id())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var items = shopping.items(selected.id());
        var accepted = catalog.status();
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("shopping-checklist.html").build().toString());
        model.addAttribute("format", format);
        model.addAttribute("listName", selected.name());
        model.addAttribute("items", items);
        model.addAttribute("summary", Summary.from(items));
        model.addAttribute("retrievedAt", accepted == null ? null : accepted.retrievedAt());
        model.addAttribute("sampleCatalogue", accepted != null && "SAMPLE".equals(accepted.origin()));
        return "offline-checklist";
    }

    @GetMapping("/offline")
    String legacyView(@RequestParam(required = false) Long listId) {
        return cart(listId);
    }

    @PostMapping({"/offline/enter", "/offline/exit"})
    String legacyAction(@RequestParam(required = false) Long listId) {
        return cart(listId);
    }

    private String cart(Long listId) {
        return listId == null ? "redirect:/list" : "redirect:/list?listId=" + listId;
    }
}
