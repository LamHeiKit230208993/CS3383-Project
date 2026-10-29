package com.pricewatch.shopping.persistence;

import com.pricewatch.catalog.persistence.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "shopping_list_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_list_item_product", columnNames = {"shopping_list_id", "product_code"}))
public class ShoppingListItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingListEntity shoppingList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_code", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean checked;

    @Column(name = "selected_retailer_code", nullable = true, length = 80)
    private String selectedRetailerCode;

    protected ShoppingListItemEntity() {
    }

    public ShoppingListItemEntity(ShoppingListEntity shoppingList, ProductEntity product, int quantity, boolean checked) {
        this(shoppingList, product, quantity, checked, null);
    }

    public ShoppingListItemEntity(ShoppingListEntity shoppingList, ProductEntity product, int quantity,
                                  boolean checked, String selectedRetailerCode) {
        this.shoppingList = shoppingList;
        this.product = product;
        this.quantity = quantity;
        this.checked = checked;
        this.selectedRetailerCode = selectedRetailerCode;
    }

    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setChecked(boolean checked) { this.checked = checked; }
    public void setSelectedRetailerCode(String selectedRetailerCode) { this.selectedRetailerCode = selectedRetailerCode; }
    public String getProductCode() { return product.getCode(); }
    public ProductEntity getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public boolean isChecked() { return checked; }
    public String getSelectedRetailerCode() { return selectedRetailerCode; }
}
