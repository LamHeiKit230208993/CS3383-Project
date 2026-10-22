package com.pricewatch.store;

import com.pricewatch.domain.Catalogue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CatalogueRepository extends JpaRepository<Catalogue, Long> {

    @Query("select distinct c from Catalogue c left join fetch c.products order by c.id")
    java.util.List<Catalogue> findWithProducts();

    default Optional<Catalogue> findFirstWithProducts() {
        return findWithProducts().stream().findFirst();
    }
}