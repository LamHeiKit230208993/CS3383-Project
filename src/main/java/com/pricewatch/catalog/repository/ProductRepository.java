package com.pricewatch.catalog.repository;

import com.pricewatch.catalog.persistence.ProductEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProductEntity product set product.active = false where product.active = true")
    int deactivateAll();

    @EntityGraph(attributePaths = {"listings", "listings.offers"})
    List<ProductEntity> findAllByActiveTrue();

    @EntityGraph(attributePaths = {"listings", "listings.offers"})
    Optional<ProductEntity> findWithListingsByCode(String code);
}
