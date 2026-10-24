package com.pricewatch.catalog.repository;

import com.pricewatch.catalog.persistence.RetailerListingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetailerListingRepository extends JpaRepository<RetailerListingEntity, Long> {
}
