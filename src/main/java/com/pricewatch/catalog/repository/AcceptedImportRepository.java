package com.pricewatch.catalog.repository;

import com.pricewatch.catalog.persistence.AcceptedImportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcceptedImportRepository extends JpaRepository<AcceptedImportEntity, Long> {
}
