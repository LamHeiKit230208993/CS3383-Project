package com.pricewatch.catalog.repository;

import com.pricewatch.catalog.persistence.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<OfferEntity, Long> {
}
