package com.pricewatch.shopping.repository;

import com.pricewatch.shopping.persistence.ShoppingListEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShoppingListRepository extends JpaRepository<ShoppingListEntity, Long> {
    Optional<ShoppingListEntity> findByName(String name);
}
