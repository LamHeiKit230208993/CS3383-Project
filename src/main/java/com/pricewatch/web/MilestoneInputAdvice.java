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

    public MilestoneInputAdvice(CatalogService catalog, HelloController format) {
        this.catalog = catalog;
        this.format = format;
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalidInput(Model model) {
        model.addAttribute("scene", "error");
        model.addAttribute("title", "Check your input");
        model.addAttribute("error", "Check the form fields. Use whole numbers for quantities and intervals, and valid decimal amounts for prices.");
        model.addAttribute("format", format);
        model.addAttribute("importStatus", catalog.status());
        return "market-desk";
    }
}
