package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class MilestoneInputAdvice {
    private final CatalogService catalog;
    private final HelloController format;
    private final UiMessages messages;

    public MilestoneInputAdvice(CatalogService catalog, HelloController format, UiMessages messages) {
        this.catalog = catalog;
        this.format = format;
        this.messages = messages;
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalidInput(Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("scene", "error");
        model.addAttribute("title", messages.get(request, "ui.title.error"));
        model.addAttribute("error", messages.get(request, "ui.error.formFields"));
        model.addAttribute("format", format);
        model.addAttribute("importStatus", catalog.status());
        return "market-desk";
    }
}
