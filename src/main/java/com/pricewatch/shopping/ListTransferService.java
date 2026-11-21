package com.pricewatch.shopping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** J10 separate-copy list sharing: export a list as JSON and merge an imported JSON copy. */
@Service
public class ListTransferService {
    private static final int MAX_ITEMS = 999;
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 999;
    private static final int MAX_CODE_LENGTH = 80;
    private static final int MAX_RETAILER_LENGTH = 80;

    private final ShoppingService shopping;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final Supplier<Instant> clock;

    public ListTransferService(ShoppingService shopping, ProductRepository productRepository,
                               ObjectMapper objectMapper, Clock clock) {
        this.shopping = shopping;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
        this.clock = clock::instant;
    }

    public byte[] export() throws JsonProcessingException {
        List<ListTransfer.ListTransferItem> items = shopping.items().stream()
                .map(item -> new ListTransfer.ListTransferItem(item.product().code(), item.quantity(),
                        item.checked(), item.selectedRetailerCode()))
                .toList();
        ListTransfer transfer = new ListTransfer(ListTransfer.FORMAT, "My list",
                Instant.ofEpochMilli(clock.get().toEpochMilli()).atZone(ZoneId.of("Asia/Hong_Kong")).toString(),
                items);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(transfer);
    }

    /** Validates the whole file first, then merges it into My list in one transaction. */
    public ImportResult importFile(byte[] bytes) {
        ListTransfer transfer = parse(bytes);
        if (transfer.items().isEmpty()) {
            throw new com.pricewatch.UiTextException("ui.error.listEmpty",
                "That list is empty; there is nothing to import.");
        }
        try {
            shopping.mergeImported(transfer.items().stream()
                    .map(item -> new ShoppingService.ImportedItem(item.code(), item.quantity(), item.checked(),
                            item.retailerCode()))
                    .toList());
        } catch (IllegalArgumentException exception) {
            throw new com.pricewatch.UiTextException("ui.error.importRolledBack",
                    "Import failed; your list was not changed. " + exception.getMessage());
        }
        return new ImportResult(transfer.items().size());
    }

    /** Preview of a chosen file, without writing anything. Items show the product name when known. */
    public Preview preview(byte[] bytes) {
        ListTransfer transfer = parse(bytes);
        Map<String, String> names = productRepository.findAllById(
                        transfer.items().stream().map(ListTransfer.ListTransferItem::code).toList()).stream()
                .collect(Collectors.toMap(product -> product.getCode(),
                        product -> product.getName() == null || product.getName().getEn() == null
                                ? product.getCode() : product.getName().getEn()));
        List<PreviewItem> items = transfer.items().stream()
                .map(item -> new PreviewItem(item.code(), names.getOrDefault(item.code(), item.code()),
                        item.quantity(), item.checked(), item.retailerCode()))
                .toList();
        return new Preview(transfer.listName(), transfer.exportedAt(), items);
    }

    ListTransfer parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new com.pricewatch.UiTextException("ui.error.fileEmpty", "The file is empty.");
        }
        try {
            ListTransfer transfer = objectMapper.readerFor(ListTransfer.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(new String(bytes, StandardCharsets.UTF_8));
            validate(transfer);
            return transfer;
        } catch (IllegalArgumentException | IOException exception) {
            if (exception instanceof IllegalArgumentException iae) {
                throw iae;
            }
            throw new com.pricewatch.UiTextException("ui.error.fileNotValid",
                "The file is not a valid Market Desk list export.");
        }
    }

    private void validate(ListTransfer transfer) {
        if (transfer == null || !ListTransfer.FORMAT.equals(transfer.format())) {
            throw new com.pricewatch.UiTextException("ui.error.fileNotMarketDesk",
                "This is not a Market Desk list export.");
        }
        if (transfer.items() == null) {
            throw new com.pricewatch.UiTextException("ui.error.fileNoItems", "The file has no item list.");
        }
        if (transfer.items().size() > MAX_ITEMS) {
            throw new com.pricewatch.UiTextException("ui.error.fileTooManyItems", "The file has too many items.");
        }
        for (ListTransfer.ListTransferItem item : transfer.items()) {
            if (item == null || item.code() == null || item.code().isBlank()
                    || item.code().length() > MAX_CODE_LENGTH
                    || !item.code().equals(item.code().trim())) {
                throw new com.pricewatch.UiTextException("ui.error.fileBadCode",
                "The file contains an invalid product code.");
            }
            if (item.quantity() < MIN_QUANTITY || item.quantity() > MAX_QUANTITY) {
                throw new com.pricewatch.UiTextException("ui.error.fileBadQuantity",
                "The file contains an invalid quantity.");
            }
            if (item.retailerCode() != null && item.retailerCode().length() > MAX_RETAILER_LENGTH) {
                throw new com.pricewatch.UiTextException("ui.error.fileBadRetailer",
                "The file contains an invalid retailer code.");
            }
        }
    }

    public record ImportResult(int itemCount) {
    }

    public record Preview(String listName, String exportedAt, List<PreviewItem> items) {
        public int itemCount() { return items.size(); }
    }

    public record PreviewItem(String code, String name, int quantity, boolean checked, String retailerCode) {
    }
}
