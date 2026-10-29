package com.pricewatch.shopping.repository;

import com.pricewatch.shopping.persistence.ShoppingListItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItemEntity, Long> {
    @EntityGraph(attributePaths = {"product", "product.listings", "product.listings.offers"})
    List<ShoppingListItemEntity> findAllByShoppingList_NameOrderByIdAsc(String listName);
    Optional<ShoppingListItemEntity> findByShoppingList_NameAndProduct_Code(String listName, String productCode);
    void deleteByShoppingList_NameAndProduct_Code(String listName, String productCode);
}
