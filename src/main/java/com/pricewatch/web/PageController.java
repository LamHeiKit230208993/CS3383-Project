package com.pricewatch.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/list";
    }

    @GetMapping("/search")
    public String search() {
        return "search";
    }

    @GetMapping("/assistant")
    public String assistant() {
        return "assistant";
    }

    @GetMapping("/alerts")
    public String alerts() {
        return "alerts";
    }

    @GetMapping("/list")
    public String list() {
        return "list";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }

    @GetMapping("/more")
    public String more() {
        return "more";
    }
}