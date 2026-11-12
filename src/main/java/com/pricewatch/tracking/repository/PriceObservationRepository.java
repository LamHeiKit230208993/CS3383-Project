package com.pricewatch.tracking.repository;

import com.pricewatch.tracking.persistence.PriceObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceObservationRepository extends JpaRepository<PriceObservationEntity, Long> {
    Optional<PriceObservationEntity> findTopByTracking_IdOrderByObservedAtDescIdDesc(Long trackingId);

    List<PriceObservationEntity> findByTracking_IdOrderByObservedAtDescIdDesc(Long trackingId);

    void deleteByTracking_Id(Long trackingId);

    void deleteByObservedAtBefore(Instant cutoff);
}
