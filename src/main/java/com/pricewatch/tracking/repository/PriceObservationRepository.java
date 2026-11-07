package com.pricewatch.tracking.repository;

import com.pricewatch.tracking.persistence.PriceObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceObservationRepository extends JpaRepository<PriceObservationEntity, Long> {
    boolean existsByProductCodeAndRetailerCodeAndChecksumAndPeriodId(
            String productCode, String retailerCode, String checksum, long periodId);

    List<PriceObservationEntity> findAllByProductCodeAndRetailerCodeOrderByRetrievedAtAscIdAsc(
            String productCode, String retailerCode);

    List<PriceObservationEntity> findAllByProductCodeOrderByRetrievedAtAscIdAsc(String productCode);

    Optional<PriceObservationEntity> findFirstByProductCodeAndRetailerCodeAndKnownTrueOrderByRetrievedAtDescIdDesc(
            String productCode, String retailerCode);
}
