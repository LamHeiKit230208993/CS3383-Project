package com.pricewatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "list_item")
public class ListItem {

    @Id
    @Column(name = "product_code", length = 64)
    private String productCode;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean checked;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected ListItem() {
    }

    public ListItem(String productCode, int quantity, boolean checked, Instant addedAt) {
        this.productCode = productCode;
        this.quantity = quantity;
        this.checked = checked;
        this.addedAt = addedAt;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
