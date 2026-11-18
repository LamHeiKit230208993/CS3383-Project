package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static com.pricewatch.web.MilestoneMessages.text;

@ControllerAdvice
public class MilestoneInputAdvice {
    private final CatalogService catalog;
    private final HelloController format;
    private final MessageSource messages;

    public MilestoneInputAdvice(CatalogService catalog, HelloController format, MessageSource messages) {
        this.catalog = catalog;
        this.format = format;
        this.messages = messages;
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalidInput(Model model) {
        model.addAttribute("scene", "error");
        model.addAttribute("title", text(messages, "error.input.title"));
        model.addAttribute("error", text(messages, "milestone.error.input"));
        model.addAttribute("format", format);
        model.addAttribute("importStatus", catalog.status());
        return "market-desk";
    }
}
