package com.pricewatch.shopping;

import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.shopping.persistence.ShoppingListEntity;
import com.pricewatch.shopping.persistence.ShoppingListItemEntity;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class ShoppingService {
    private static final String LIST_NAME = "My list";
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 999;

    private final ShoppingListRepository listRepository;
    private final ShoppingListItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;
    private final Object mutationLock = new Object();

    public ShoppingService(ShoppingListRepository listRepository,
                           ShoppingListItemRepository itemRepository,
                           ProductRepository productRepository,
                           PlatformTransactionManager transactionManager) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.productRepository = productRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<ItemView> items() {
        return itemRepository.findAllByShoppingList_NameOrderByIdAsc(LIST_NAME).stream()
                .map(this::toView)
                .toList();
    }

    public void add(String productCode, int quantity) {
        add(productCode, quantity, null);
    }

    public void add(String productCode, int quantity, String retailerCode) {
        validateQuantity(quantity);
        String requestedRetailer = automaticIfBlank(retailerCode);
        mutate(() -> {
            ProductEntity product = productRepository.findById(productCode)
                    .orElseThrow(() -> new IllegalArgumentException("That product is not in the accepted catalogue."));
            if (!product.isActive()) {
                throw new IllegalArgumentException("That product is no longer in the current catalogue.");
            }
            ShoppingListEntity list = getOrCreateList();
            ShoppingListItemEntity existing = itemRepository
                    .findByShoppingList_NameAndProduct_Code(LIST_NAME, productCode).orElse(null);
            if (existing != null) {
                if (!Objects.equals(existing.getSelectedRetailerCode(), requestedRetailer)) {
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

    public void update(String productCode, int quantity, boolean checked) {
        validateQuantity(quantity);
        mutate(() -> {
            ShoppingListItemEntity item = itemRepository
                    .findByShoppingList_NameAndProduct_Code(LIST_NAME, productCode)
                    .orElseThrow(() -> new IllegalArgumentException("That item is not on your list."));
            item.setQuantity(quantity);
            item.setChecked(checked);
        });
    }

    public void update(String productCode, int quantity, boolean checked, String retailerCode) {
        validateQuantity(quantity);
        String requestedRetailer = automaticIfBlank(retailerCode);
        mutate(() -> {
            ShoppingListItemEntity item = itemRepository
                    .findByShoppingList_NameAndProduct_Code(LIST_NAME, productCode)
                    .orElseThrow(() -> new IllegalArgumentException("That item is not on your list."));
            if (!Objects.equals(item.getSelectedRetailerCode(), requestedRetailer)) {
                if (requestedRetailer != null) {
                    validateRetailer(item.getProduct(), requestedRetailer);
                }
                item.setSelectedRetailerCode(requestedRetailer);
            }
            item.setQuantity(quantity);
            item.setChecked(checked);
        });
    }

    public void remove(String productCode) {
        mutate(() -> itemRepository.deleteByShoppingList_NameAndProduct_Code(LIST_NAME, productCode));
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
