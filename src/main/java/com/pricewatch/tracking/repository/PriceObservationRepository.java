package com.pricewatch.tracking.repository;

import com.pricewatch.tracking.PriceHistoryEntry;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceObservationRepository extends JpaRepository<PriceObservationEntity, Long> {
    Optional<PriceObservationEntity> findTopByTracking_IdOrderByObservedAtDescIdDesc(Long trackingId);

    List<PriceObservationEntity> findByTracking_IdOrderByObservedAtDescIdDesc(Long trackingId);

    @Query("""
            select new com.pricewatch.tracking.PriceHistoryEntry(t.retailerCode, o.observedAt, o.price)
            from PriceObservationEntity o join o.tracking t
            where t.product.code = :code and o.observedAt >= :cutoff
            order by o.observedAt desc, t.retailerCode, o.id desc
            """)
    List<PriceHistoryEntry> historyForProduct(String code, Instant cutoff);

    void deleteByTracking_Id(Long trackingId);

    void deleteByObservedAtBefore(Instant cutoff);
}
