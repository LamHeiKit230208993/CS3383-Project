package com.pricewatch.sharing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.i18n.AppMessages;
import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.persistence.ShoppingListEntity;
import com.pricewatch.shopping.persistence.ShoppingListItemEntity;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Versioned list transfer. Export is a snapshot, not a live link. Import parses
 * and validates the whole file before it creates one new list; a failure never
 * changes an existing list.
 */
@Service
public class ListTransferService {
    public static final String FORMAT = "pricewatch-list";
    public static final int VERSION = 1;
    public static final int MAX_ITEMS = 999;
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final String MAIN_LIST = "My list";
    private static final int MAX_NAME_LENGTH = 80;

    private final ObjectMapper mapper;
    private final ShoppingListRepository listRepository;
    private final ShoppingListItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final ShoppingService shopping;
    private final Clock clock;

    public ListTransferService(ObjectMapper mapper,
                               ShoppingListRepository listRepository,
                               ShoppingListItemRepository itemRepository,
                               ProductRepository productRepository,
                               ShoppingService shopping,
                               Clock clock) {
        this.mapper = mapper;
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.productRepository = productRepository;
        this.shopping = shopping;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ListSummary> lists() {
        return listRepository.findAllByOrderByIdAsc().stream()
                .map(list -> new ListSummary(list.getId(), list.getName(),
                        (int) itemRepository.countByShoppingList_Id(list.getId()),
                        MAIN_LIST.equals(list.getName())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExportFile export(long listId) {
        ShoppingListEntity list = findList(listId);
        return toFile(list.getName(), shopping.items(listId));
    }

    @Transactional(readOnly = true)
    public ExportFile exportMainList() {
        return listRepository.findByName(MAIN_LIST)
                .map(list -> toFile(list.getName(), shopping.items()))
                .orElseGet(() -> toFile(MAIN_LIST, List.of()));
    }

    @Transactional(readOnly = true)
    public ListPreview preview(long listId) {
        ShoppingListEntity list = findList(listId);
        return new ListPreview(list.getId(), list.getName(), shopping.items(listId));
    }

    @Transactional
    public ListPreview importFile(byte[] bytes) {
        ParsedFile parsed = parse(bytes);
        ShoppingListEntity list = listRepository.saveAndFlush(new ShoppingListEntity(uniqueName(parsed.name())));
        for (ParsedItem item : parsed.items()) {
            itemRepository.save(new ShoppingListItemEntity(list, item.product(), item.quantity(),
                    item.checked(), item.retailerCode()));
        }
        itemRepository.flush();
        return new ListPreview(list.getId(), list.getName(), shopping.items(list.getId()));
    }

    private ExportFile toFile(String name, List<ItemView> items) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("format", FORMAT);
        file.put("version", VERSION);
        file.put("exportedAt", clock.instant().toString());
        file.put("name", name);
        file.put("items", items.stream().map(item -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("productCode", item.product().code());
            entry.put("quantity", item.quantity());
            entry.put("checked", item.checked());
            entry.put("retailerCode", item.selectedRetailerCode());
            return entry;
        }).toList());
        try {
            return new ExportFile(name, mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(file));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write the list transfer file.", exception);
        }
    }

    private ParsedFile parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException(AppMessages.text("error.share.emptyFile"));
        }
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException(AppMessages.text("error.share.tooLarge"));
        }
        JsonNode root;
        try {
            root = mapper.readTree(bytes);
        } catch (IOException exception) {
            throw new IllegalArgumentException(AppMessages.text("error.share.invalid"));
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException(AppMessages.text("error.share.invalid"));
        }
        if (!FORMAT.equals(root.path("format").asText())) {
            throw new IllegalArgumentException(AppMessages.text("error.share.format"));
        }
        JsonNode version = root.get("version");
        if (version == null || !version.isIntegralNumber()
                || !version.canConvertToInt() || version.asInt() != VERSION) {
            throw new IllegalArgumentException(AppMessages.text("error.share.version", VERSION));
        }
        JsonNode nodes = root.get("items");
        if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
            throw new IllegalArgumentException(AppMessages.text("error.share.items"));
        }
        if (nodes.size() > MAX_ITEMS) {
            throw new IllegalArgumentException(AppMessages.text("error.share.tooManyItems", MAX_ITEMS));
        }

        String name = requestedName(root.get("name"));
        List<ParsedItem> items = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        for (JsonNode node : nodes) {
            if (!node.isObject()) {
                throw new IllegalArgumentException(AppMessages.text("error.share.invalid"));
            }
            String code = text(node.get("productCode"));
            if (code == null) {
                throw new IllegalArgumentException(AppMessages.text("error.share.product", "?"));
            }
            if (!codes.add(code)) {
                throw new IllegalArgumentException(AppMessages.text("error.share.duplicate", code));
            }
            JsonNode quantityNode = node.get("quantity");
            if (quantityNode == null || !quantityNode.isIntegralNumber() || !quantityNode.canConvertToInt()) {
                throw new IllegalArgumentException(AppMessages.text("error.share.quantity", code));
            }
            int quantity = quantityNode.asInt();
            if (quantity < 1 || quantity > 999) {
                throw new IllegalArgumentException(AppMessages.text("error.share.quantity", code));
            }
            JsonNode checkedNode = node.get("checked");
            if (checkedNode == null || !checkedNode.isBoolean()) {
                throw new IllegalArgumentException(AppMessages.text("error.share.checked", code));
            }
            String retailer = retailerCode(node.get("retailerCode"), code);

            ProductEntity product = productRepository.findWithListingsByCode(code).orElse(null);
            if (product == null || !product.isActive()) {
                throw new IllegalArgumentException(AppMessages.text("error.share.product", code));
            }
            if (retailer != null && product.getListings().stream()
                    .noneMatch(listing -> listing.getRetailerCode().equals(retailer))) {
                // Membership is required; a listed retailer may have a null
                // price, and that saved choice is accepted below.
                throw new IllegalArgumentException(AppMessages.text("error.share.retailer", code, retailer));
            }
            items.add(new ParsedItem(product, quantity, checkedNode.booleanValue(), retailer));
        }
        return new ParsedFile(name, items);
    }

    private String retailerCode(JsonNode node, String code) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new IllegalArgumentException(AppMessages.text("error.share.retailer", code, "?"));
        }
        String value = node.asText().strip();
        return value.isEmpty() ? null : value;
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return null;
        }
        String value = node.asText().strip();
        return value.isEmpty() ? null : value;
    }

    private static String requestedName(JsonNode node) {
        String requested = node != null && node.isTextual() ? node.asText() : "";
        requested = requested.replaceAll("\\p{Cntrl}", " ").strip().replaceAll("\\s+", " ");
        if (requested.isEmpty()) {
            requested = AppMessages.text("share.defaultName");
        }
        if (requested.codePointCount(0, requested.length()) > MAX_NAME_LENGTH) {
            requested = requested.substring(0, requested.offsetByCodePoints(0, MAX_NAME_LENGTH)).strip();
        }
        return requested;
    }

    private String uniqueName(String requested) {
        String base = requested;
        if (!listRepository.existsByName(base)) return base;
        String candidate = base + " (imported)";
        if (!listRepository.existsByName(candidate)) return candidate;
        for (int number = 2; number < 10_000; number++) {
            candidate = base + " (imported " + number + ")";
            if (!listRepository.existsByName(candidate)) return candidate;
        }
        throw new IllegalArgumentException(AppMessages.text("error.share.name"));
    }

    private ShoppingListEntity findList(long listId) {
        return listRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException(AppMessages.text("error.share.listNotFound")));
    }

    private record ParsedFile(String name, List<ParsedItem> items) {
    }

    private record ParsedItem(ProductEntity product, int quantity, boolean checked, String retailerCode) {
    }
}
