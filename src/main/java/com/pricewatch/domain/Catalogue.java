package com.pricewatch.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Entity
@Table(name = "catalogue")
public class Catalogue {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    private Instant importedAt;

    private LocalDate sourceDate;

    private String sourceUrl;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "catalogue_id")
    private List<Product> products = new ArrayList<>();

    protected Catalogue() {
    }

    public Catalogue(Instant importedAt, LocalDate sourceDate, String sourceUrl, List<Product> products) {
        this.importedAt = importedAt;
        this.sourceDate = sourceDate;
        this.sourceUrl = sourceUrl;
        this.products = new ArrayList<>(products);
    }

    public Long getId() {
        return id;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public LocalDate getSourceDate() {
        return sourceDate;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public List<Product> getProducts() {
        return List.copyOf(products);
    }

    public Map<String, Product> byCode() {
        Map<String, Product> map = new LinkedHashMap<>();
        for (Product product : products) {
            map.put(product.getCode(), product);
        }
        return map;
    }

    public List<Product> find(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return List.of();
        }
        return products.stream()
                .filter(product -> product.searchableText().stream()
                        .anyMatch(text -> text != null && text.toLowerCase(Locale.ROOT).contains(needle)))
                .toList();
    }

    public Product product(String code) {
        for (Product product : products) {
            if (product.getCode().equals(code)) {
                return product;
            }
        }
        return null;
    }
}
