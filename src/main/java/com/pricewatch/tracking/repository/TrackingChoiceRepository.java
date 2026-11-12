package com.pricewatch.tracking.repository;

import com.pricewatch.tracking.persistence.TrackingChoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackingChoiceRepository extends JpaRepository<TrackingChoiceEntity, Long> {
    List<TrackingChoiceEntity> findAllByProductCode(String productCode);
    List<TrackingChoiceEntity> findAllByEnabledTrue();
    Optional<TrackingChoiceEntity> findByProductCodeAndRetailerCode(String productCode, String retailerCode);
}
