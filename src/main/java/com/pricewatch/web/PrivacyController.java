package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.i18n.AppMessages;
import com.pricewatch.privacy.PersonalDataService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
public class PrivacyController {
    private static final String EXPORT_FILE = "pricewatch-personal-data.json";

    private final PersonalDataService personalData;
    private final CatalogService catalog;
    private final HelloController formatter;

    public PrivacyController(PersonalDataService personalData, CatalogService catalog, HelloController formatter) {
        this.personalData = personalData;
        this.catalog = catalog;
        this.formatter = formatter;
    }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", formatter);
    }

    @GetMapping("/privacy")
    String page(Model model) {
        model.addAttribute("summary", personalData.summary());
        model.addAttribute("scene", "privacy");
        model.addAttribute("title", AppMessages.text("page.privacy"));
        return "market-desk";
    }

    @GetMapping("/privacy/export")
    ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(EXPORT_FILE, StandardCharsets.UTF_8)
                        .build().toString())
                .body(personalData.export());
    }

    @PostMapping("/privacy/delete")
    String delete(@RequestParam(defaultValue = "false") boolean confirmDelete, RedirectAttributes redirect) {
        if (!confirmDelete) {
            redirect.addFlashAttribute("error", AppMessages.text("privacy.deleteNeedsConfirmation"));
            return "redirect:/privacy";
        }
        personalData.deleteAll();
        redirect.addFlashAttribute("notice", AppMessages.text("privacy.deleted"));
        return "redirect:/privacy";
    }
}
