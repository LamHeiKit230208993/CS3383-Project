package com.pricewatch.tracking.repository;

import com.pricewatch.tracking.persistence.WishlistEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistEntryRepository extends JpaRepository<WishlistEntryEntity, String> {
}
