package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** J12 R45 and R10: local personal-data controls and the price history view. */
@Controller
public class PersonalDataController {
    private final PersonalDataService personalData;
    private final HistoryService history;
    private final CatalogService catalog;
    private final HelloController format;
    private final UiMessages messages;

    public PersonalDataController(PersonalDataService personalData, HistoryService history,
                                  CatalogService catalog, HelloController format, UiMessages messages) {
        this.personalData = personalData;
        this.history = history;
        this.catalog = catalog;
        this.format = format;
        this.messages = messages;
    }

    @GetMapping("/my-data")
    String myData(Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        model.addAttribute("overview", personalData.overview());
        return screen(model, request, "my-data", "ui.title.myData");
    }

    @GetMapping("/my-data/export")
    ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"market-desk-my-data.json\"")
                .body(personalData.export());
    }

    @PostMapping("/my-data/delete")
    String delete(@RequestParam(defaultValue = "") String confirm, RedirectAttributes redirect,
                  jakarta.servlet.http.HttpServletRequest request) {
        if (!"DELETE".equals(confirm.strip())) {
            redirect.addFlashAttribute("error", messages.get(request, "ui.error.deleteConfirm"));
            return "redirect:/my-data";
        }
        personalData.deleteLocalData();
        redirect.addFlashAttribute("notice", messages.get(request, "ui.notice.dataDeleted"));
        return "redirect:/my-data";
    }

    @GetMapping("/history")
    String history(Model model, jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", format);
        var products = history.products();
        model.addAttribute("historyProducts", products);
        model.addAttribute("hasHistory", !products.isEmpty());
        return screen(model, request, "history", "ui.title.history");
    }

    private String screen(Model model, jakarta.servlet.http.HttpServletRequest request,
                          String scene, String titleKey) {
        model.addAttribute("scene", scene);
        model.addAttribute("title", messages.get(request, titleKey));
        return "market-desk";
    }
}
