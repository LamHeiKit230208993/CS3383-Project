package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.ShoppingService.ListChoice;
import com.pricewatch.shopping.Summary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Controller
class OfflineListController {
    static final String SESSION_KEY = OfflineListController.class.getName() + ".review";
    private final ShoppingService shopping;
    private final CatalogService catalog;
    private final HelloController format;
    private final Clock clock;

    OfflineListController(ShoppingService shopping, CatalogService catalog, HelloController format, Clock clock) {
        this.shopping = shopping;
        this.catalog = catalog;
        this.format = format;
        this.clock = clock;
    }

    @PostMapping("/offline/enter")
    String enter(@RequestParam(required = false) Long listId, HttpSession session) {
        var selected = select(shopping.lists(), listId);
        if (selected == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        session.setAttribute(SESSION_KEY, new Review(selected.id()));
        return "redirect:/offline";
    }

    @GetMapping("/offline")
    String view(HttpServletRequest request, HttpServletResponse response, Model model) {
        Review review = review(request);
        if (review == null) return "redirect:/list";
        response.setHeader("Cache-Control", "no-store");
        var choices = shopping.lists();
        var selected = select(choices, review.listId());
        if (selected == null) selected = choices.getFirst();
        var items = shopping.items(selected.id());
        var accepted = catalog.status();
        var retrievedAt = accepted == null ? null : accepted.retrievedAt();
        model.addAttribute("scene", "offline");
        model.addAttribute("title", "Offline review");
        model.addAttribute("format", format);
        model.addAttribute("listName", selected.name());
        model.addAttribute("items", items);
        model.addAttribute("summary", Summary.from(items));
        model.addAttribute("retrievedAt", retrievedAt);
        model.addAttribute("sampleCatalogue", accepted != null && "SAMPLE".equals(accepted.origin()));
        model.addAttribute("ageHours", retrievedAt == null ? null
                : Math.max(0, Duration.between(retrievedAt, clock.instant()).toHours()));
        return "market-desk";
    }

    @PostMapping("/offline/exit")
    String exit(HttpServletRequest request) {
        var review = review(request);
        if (review == null) return "redirect:/list";
        request.getSession(false).removeAttribute(SESSION_KEY);
        var selected = select(shopping.lists(), review.listId());
        return selected == null || selected.id() == null ? "redirect:/list"
                : "redirect:/list?listId=" + selected.id();
    }

    static Review review(HttpServletRequest request) {
        var session = request.getSession(false);
        return session == null ? null : (Review) session.getAttribute(SESSION_KEY);
    }

    private ListChoice select(List<ListChoice> choices, Long id) {
        return id == null ? choices.getFirst() : choices.stream()
                .filter(choice -> id.equals(choice.id())).findFirst().orElse(null);
    }

    record Review(Long listId) {}
}
