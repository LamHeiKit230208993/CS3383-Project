package com.pricewatch.shopping;

import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.catalog.persistence.LocalizedLabel;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.shopping.persistence.ShoppingListEntity;
import com.pricewatch.shopping.persistence.ShoppingListItemEntity;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
public class ShoppingService {
    public static final String DONE_ITEM_ERROR = "Reopen this Done item in Cart before changing it.";
    private static final String LIST_NAME = "My list";
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 999;

    private final ShoppingListRepository listRepository;
    private final ShoppingListItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final Object mutationLock = new Object();

    public ShoppingService(ShoppingListRepository listRepository,
                           ShoppingListItemRepository itemRepository,
                           ProductRepository productRepository,
                           EntityManager entityManager,
                           PlatformTransactionManager transactionManager) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<ItemView> items() {
        return items(null);
    }

    @Transactional(readOnly = true)
    public List<ItemView> items(Long listId) {
        return itemRepository.findAllByShoppingList_NameOrderByIdAsc(listName(listId)).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ListChoice> lists() {
        var lists = listRepository.findAll().stream()
                .map(list -> new ListChoice(list.getId(), list.getName()))
                .sorted(Comparator.comparing((ListChoice list) -> !LIST_NAME.equals(list.name()))
                        .thenComparing(ListChoice::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (!lists.isEmpty() && LIST_NAME.equals(lists.getFirst().name())) return lists;
        var choices = new ArrayList<ListChoice>();
        choices.add(new ListChoice(null, LIST_NAME));
        choices.addAll(lists);
        return List.copyOf(choices);
    }

    @Transactional(readOnly = true)
    public ListTransfer exportList(Long listId) {
        String name = listName(listId);
        var rows = itemRepository.findAllByShoppingList_NameOrderByIdAsc(name).stream()
                .map(item -> new ListTransfer.Item(item.getProductCode(), displayName(item.getProduct()),
                        item.getQuantity(), item.isChecked(), item.getSelectedRetailerCode()))
                .toList();
        return ListTransfer.forExport(name, rows);
    }

    public long importList(InputStream input) {
        var transfer = ListTransfer.read(input);
        synchronized (mutationLock) {
            return Objects.requireNonNull(transactionTemplate.execute(status -> saveImportedList(transfer)));
        }
    }

    private long saveImportedList(ListTransfer transfer) {
        var list = listRepository.save(new ShoppingListEntity(availableName(transfer.name())));
        var products = new HashMap<String, ProductEntity>();
        productRepository.findAllById(transfer.items().stream().map(ListTransfer.Item::productCode).toList())
                .forEach(product -> products.put(product.getCode(), product));
        var items = new ArrayList<ShoppingListItemEntity>();
        for (ListTransfer.Item row : transfer.items()) {
            var product = products.get(row.productCode());
            if (product == null) {
                product = new ProductEntity(row.productCode());
                product.setName(new LocalizedLabel(row.displayName(), "", ""));
                product.setImportedListReference(true);
                // An insert cannot overwrite a source product added by a concurrent catalogue refresh.
                entityManager.persist(product);
            }
            items.add(new ShoppingListItemEntity(list, product, row.quantity(), row.checked(), row.retailerCode()));
        }
        itemRepository.saveAll(items);
        return list.getId();
    }

    private String availableName(String requested) {
        if (!reservedName(requested) && !listRepository.existsByNameIgnoreCase(requested)) return requested;
        for (int number = 2; ; number++) {
            String suffix = " (" + number + ")";
            String base = requested.substring(0, Math.min(requested.length(), 100 - suffix.length())).stripTrailing();
            if (Character.isHighSurrogate(base.charAt(base.length() - 1))) {
                base = base.substring(0, base.length() - 1).stripTrailing();
            }
            String candidate = base + suffix;
            if (!listRepository.existsByNameIgnoreCase(candidate)) return candidate;
        }
    }

    private String displayName(ProductEntity product) {
        var label = product.getName();
        if (label != null) {
            for (String name : new String[]{label.getEn(), label.getZhHant(), label.getZhHans()}) {
                if (name == null) continue;
                String displayName = name.replaceAll("\\p{IsControl}", " ").strip();
                if (!displayName.isBlank()) return displayName;
            }
        }
        return product.getCode();
    }

    private String listName(Long listId) {
        if (listId == null) return LIST_NAME;
        return findList(listId).getName();
    }

    public record ListChoice(Long id, String name) {}

    public long createList(String name) {
        synchronized (mutationLock) {
            return Objects.requireNonNull(transactionTemplate.execute(status ->
                    listRepository.save(new ShoppingListEntity(newListName(name, null))).getId()));
        }
    }

    public void renameList(long listId, String name) {
        mutate(() -> {
            var list = manageableList(listId);
            list.rename(newListName(name, list));
        });
    }

    public void deleteList(long listId) {
        mutate(() -> {
            var list = manageableList(listId);
            itemRepository.deleteByShoppingList_Id(listId);
            listRepository.delete(list);
        });
    }

    private ShoppingListEntity findList(long listId) {
        return listRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("That shopping list does not exist."));
    }

    private ShoppingListEntity manageableList(long listId) {
        var list = findList(listId);
        if (LIST_NAME.equals(list.getName())) {
            throw new IllegalArgumentException("The main Cart cannot be renamed or deleted.");
        }
        return list;
    }

    private String newListName(String name, ShoppingListEntity existing) {
        String normalized = name == null ? "" : name.strip();
        if (normalized.isBlank() || normalized.length() > 100 || normalized.chars().anyMatch(Character::isISOControl)
                || reservedName(normalized)) {
            throw new IllegalArgumentException("Enter a list name from 1 to 100 characters that is not reserved.");
        }
        if ((existing == null || !existing.getName().equalsIgnoreCase(normalized))
                && listRepository.existsByNameIgnoreCase(normalized)) {
            throw new IllegalArgumentException("A shopping list already has that name.");
        }
        return normalized;
    }

    private static boolean reservedName(String name) {
        return LIST_NAME.equalsIgnoreCase(name) || "Cart".equalsIgnoreCase(name);
    }

    public void add(String productCode, int quantity) {
        add(productCode, quantity, null);
    }

    public void add(String productCode, int quantity, String retailerCode) {
        add(null, productCode, quantity, retailerCode == null ? "" : retailerCode);
    }

    public void add(Long listId, String productCode, int quantity, String retailerCode) {
        validateQuantity(quantity);
        String requestedRetailer = automaticIfBlank(retailerCode);
        mutate(() -> {
            ProductEntity product = productRepository.findById(productCode)
                    .orElseThrow(() -> new IllegalArgumentException("That product is not in the accepted catalogue."));
            if (!product.isActive()) {
                throw new IllegalArgumentException("That product is no longer in the current catalogue.");
            }
            ShoppingListEntity list = listId == null ? getOrCreateList() : findList(listId);
            ShoppingListItemEntity existing = itemRepository
                    .findByShoppingList_NameAndProduct_Code(list.getName(), productCode).orElse(null);
            if (existing != null) {
                requireOpen(existing);
                if (retailerCode != null && !Objects.equals(existing.getSelectedRetailerCode(), requestedRetailer)) {
                    throw new IllegalArgumentException("This product is already in your list with another retailer; "
                            + "change retailer in My list.");
                }
                int combined = existing.getQuantity() + quantity;
                validateQuantity(combined);
                existing.setQuantity(combined);
            } else {
                if (requestedRetailer != null) {
                    validateRetailer(product, requestedRetailer);
                }
                itemRepository.save(new ShoppingListItemEntity(list, product, quantity, false, requestedRetailer));
            }
        });
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void addFromReminder(String productCode, int quantity, String retailerCode) {
        // Commit the Cart write before releasing its mutation lock even when a reminder row is locked.
        add(productCode, quantity, retailerCode);
    }

    public BatchAddResult addMissingProducts(List<String> productCodes) {
        synchronized (mutationLock) {
            return transactionTemplate.execute(status -> {
                int added = 0;
                int existing = 0;
                int unavailable = 0;
                for (String code : productCodes.stream().distinct().toList()) {
                    var product = productRepository.findById(code);
                    if (product.isEmpty() || !product.get().isActive()) {
                        unavailable++;
                    } else if (itemRepository.findByShoppingList_NameAndProduct_Code(LIST_NAME, code).isPresent()) {
                        existing++;
                    } else {
                        itemRepository.save(new ShoppingListItemEntity(getOrCreateList(), product.get(), 1, false, null));
                        added++;
                    }
                }
                return new BatchAddResult(added, existing, unavailable);
            });
        }
    }

    public record BatchAddResult(int added, int existing, int unavailable) {}

    public void update(String productCode, int quantity, boolean checked) {
        validateQuantity(quantity);
        mutate(() -> {
            ShoppingListItemEntity item = itemRepository
                    .findByShoppingList_NameAndProduct_Code(LIST_NAME, productCode)
                    .orElseThrow(() -> new IllegalArgumentException("That item is not on your list."));
            requireOpen(item);
            item.setQuantity(quantity);
            item.setChecked(checked);
        });
    }

    public void update(String productCode, int quantity, boolean checked, String retailerCode) {
        update(null, productCode, quantity, checked, retailerCode);
    }

    public void update(Long listId, String productCode, int quantity, boolean checked, String retailerCode) {
        edit(listId, productCode, quantity, checked, retailerCode);
    }

    public void editDetails(Long listId, String productCode, int quantity, String retailerCode) {
        edit(listId, productCode, quantity, null, retailerCode);
    }

    private void edit(Long listId, String productCode, int quantity, Boolean checked, String retailerCode) {
        validateQuantity(quantity);
        String requestedRetailer = automaticIfBlank(retailerCode);
        mutate(() -> {
            ShoppingListItemEntity item = itemRepository
                    .findByShoppingList_NameAndProduct_Code(listName(listId), productCode)
                    .orElseThrow(() -> new IllegalArgumentException("That item is not on your list."));
            requireOpen(item);
            if (!Objects.equals(item.getSelectedRetailerCode(), requestedRetailer)) {
                if (requestedRetailer != null) {
                    validateRetailer(item.getProduct(), requestedRetailer);
                }
                item.setSelectedRetailerCode(requestedRetailer);
            }
            item.setQuantity(quantity);
            if (checked != null) item.setChecked(checked);
        });
    }

    public void checkOff(Long listId, String productCode, boolean checked) {
        mutate(() -> itemRepository.findByShoppingList_NameAndProduct_Code(listName(listId), productCode)
                .orElseThrow(() -> new IllegalArgumentException("That item is not on your list."))
                .setChecked(checked));
    }

    public void remove(String productCode) {
        remove(null, productCode);
    }

    public void remove(Long listId, String productCode) {
        mutate(() -> itemRepository.findByShoppingList_NameAndProduct_Code(listName(listId), productCode)
                .ifPresent(item -> {
                    requireOpen(item);
                    itemRepository.delete(item);
                }));
    }

    private static void requireOpen(ShoppingListItemEntity item) {
        if (item.isChecked()) throw new IllegalArgumentException(DONE_ITEM_ERROR);
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        return Summary.from(items());
    }

    private ShoppingListEntity getOrCreateList() {
        return listRepository.findByName(LIST_NAME)
                .orElseGet(() -> listRepository.save(new ShoppingListEntity(LIST_NAME)));
    }

    private void mutate(Runnable operation) {
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status -> operation.run());
        }
    }

    private ItemView toView(ShoppingListItemEntity item) {
        ProductView product = ProductView.from(item.getProduct());
        String selectedRetailer = item.getSelectedRetailerCode();
        ListingView effectiveListing = selectedRetailer == null
                ? product.lowestListings().stream().findFirst().orElse(null)
                : product.listings().stream()
                        .filter(listing -> listing.retailerCode().equals(selectedRetailer))
                        .findFirst().orElse(null);
        BigDecimal unitPrice = effectiveListing == null ? null : effectiveListing.price();
        BigDecimal lineTotal = unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        String retailer = selectedRetailer != null ? selectedRetailer
                : effectiveListing == null ? null : effectiveListing.retailerCode();
        return new ItemView(product, item.getQuantity(), item.isChecked(), unitPrice, retailer, lineTotal,
                selectedRetailer);
    }

    private void validateRetailer(ProductEntity product, String retailerCode) {
        boolean available = product.getListings().stream()
                .anyMatch(listing -> listing.getRetailerCode().equals(retailerCode) && listing.getPrice() != null);
        if (!available) {
            throw new IllegalArgumentException("Choose a retailer with a current price for this product.");
        }
    }

    private String automaticIfBlank(String retailerCode) {
        return retailerCode == null || retailerCode.isBlank() ? null : retailerCode;
    }

    private void validateQuantity(int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be a whole number from 1 to 999.");
        }
    }
}
