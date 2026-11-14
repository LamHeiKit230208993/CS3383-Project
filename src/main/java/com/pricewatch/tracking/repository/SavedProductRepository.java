package com.pricewatch.tracking.repository;

import com.pricewatch.tracking.persistence.SavedProductEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedProductRepository extends JpaRepository<SavedProductEntity, String> {
    @EntityGraph(attributePaths = {"product", "product.listings", "product.listings.offers"})
    List<SavedProductEntity> findAllByOrderBySavedAtDesc();
}
