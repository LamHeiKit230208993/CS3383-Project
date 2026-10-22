package com.pricewatch.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_list")
public class ShoppingList {

    public static final long SINGLETON_ID = 1L;
    public static final String DEFAULT_NAME = "Weekly essentials";

    @Id
    private Long id = SINGLETON_ID;

    private String name = DEFAULT_NAME;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "list_id")
    @OrderBy("addedAt ASC")
    private List<ListItem> items = new ArrayList<>();

    protected ShoppingList() {
    }

    public ShoppingList(String name, List<ListItem> items) {
        this.name = name;
        this.items = new ArrayList<>(items);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<ListItem> getItems() {
        return List.copyOf(items);
    }

    public ListItem item(String code) {
        for (ListItem item : items) {
            if (item.getProductCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    public ListItem add(String code, int quantity, Instant now) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        ListItem existing = item(code);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            return existing;
        }
        ListItem created = new ListItem(code, quantity, false, now);
        items.add(created);
        return created;
    }

    public void changeQuantity(String code, int quantity) {
        ListItem existing = item(code);
        if (existing == null) {
            throw new IllegalArgumentException("Item is not in the list");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        existing.setQuantity(quantity);
    }

    public void toggle(String code, boolean checked) {
        ListItem existing = item(code);
        if (existing == null) {
            throw new IllegalArgumentException("Item is not in the list");
        }
        existing.setChecked(checked);
    }

    public void remove(String code) {
        ListItem existing = item(code);
        if (existing == null) {
            throw new IllegalArgumentException("Item is not in the list");
        }
        items.remove(existing);
    }

    public int checkedCount() {
        return (int) items.stream().filter(ListItem::isChecked).count();
    }
}
