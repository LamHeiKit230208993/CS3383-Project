package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.i18n.AppMessages;
import com.pricewatch.sharing.ExportFile;
import com.pricewatch.sharing.ListPreview;
import com.pricewatch.sharing.ListTransferService;
import com.pricewatch.sharing.OfflineSnapshot;
import com.pricewatch.sharing.OfflineSnapshotService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * List transfer and offline pages. Export and offline snapshots never change
 * data; import only ever creates a new list, so an existing list is never
 * overwritten or merged.
 */
@Controller
public class ListTransferController {
    private final ListTransferService transfer;
    private final OfflineSnapshotService offlineSnapshots;
    private final CatalogService catalog;
    private final HelloController formatter;

    public ListTransferController(ListTransferService transfer,
                                  OfflineSnapshotService offlineSnapshots,
                                  CatalogService catalog,
                                  HelloController formatter) {
        this.transfer = transfer;
        this.offlineSnapshots = offlineSnapshots;
        this.catalog = catalog;
        this.formatter = formatter;
    }

    @ModelAttribute
    void common(Model model) {
        model.addAttribute("importStatus", catalog.status());
        model.addAttribute("format", formatter);
    }

    @GetMapping("/list/export")
    ResponseEntity<byte[]> export(@RequestParam(required = false) Long listId) {
        ExportFile file;
        try {
            file = listId == null ? transfer.exportMainList() : transfer.export(listId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName(file.name(), ".pricewatch.json"), StandardCharsets.UTF_8)
                        .build().toString())
                .body(file.bytes());
    }

    @GetMapping("/list/offline")
    ResponseEntity<byte[]> offline(@RequestParam(required = false) Long listId) {
        OfflineSnapshot snapshot;
        try {
            snapshot = offlineSnapshots.snapshot(listId, LocaleContextHolder.getLocale());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName(snapshot.name(), ".pricewatch.html"), StandardCharsets.UTF_8)
                        .build().toString())
                .body(snapshot.bytes());
    }

    @GetMapping("/list/import")
    String page(@RequestParam(required = false) Long listId, Model model) {
        ListPreview preview = null;
        if (listId != null) {
            try {
                preview = transfer.preview(listId);
            } catch (IllegalArgumentException exception) {
                model.addAttribute("error", exception.getMessage());
            }
        }
        return show(model, preview);
    }

    @PostMapping("/list/import")
    String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException(AppMessages.text("error.share.emptyFile"));
            }
            ListPreview preview = transfer.importFile(file.getBytes());
            redirect.addFlashAttribute("notice", AppMessages.text("notice.share.imported", preview.name()));
            return "redirect:/list/import?listId=" + preview.id();
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        } catch (IOException exception) {
            redirect.addFlashAttribute("error", AppMessages.text("error.share.invalid"));
        }
        return "redirect:/list/import";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    String tooLarge(RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", AppMessages.text("error.share.tooLarge"));
        return "redirect:/list/import";
    }

    private String show(Model model, ListPreview preview) {
        model.addAttribute("lists", transfer.lists());
        model.addAttribute("preview", preview);
        model.addAttribute("scene", "sharing");
        model.addAttribute("title", AppMessages.text("page.share"));
        return "market-desk";
    }

    private static String fileName(String name, String suffix) {
        String base = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "-").strip();
        if (base.isEmpty()) base = "pricewatch-list";
        return base + suffix;
    }
}
