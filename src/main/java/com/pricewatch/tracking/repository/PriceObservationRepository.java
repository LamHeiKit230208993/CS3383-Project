package com.pricewatch.tracking.repository;

import com.pricewatch.tracking.persistence.PriceObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceObservationRepository extends JpaRepository<PriceObservationEntity, Long> {
    List<PriceObservationEntity> findAllByProductCodeAndRetailerCodeOrderByRetrievedAtAscIdAsc(
            String productCode, String retailerCode);
    List<PriceObservationEntity> findAllByProductCodeOrderByRetrievedAtDescIdDesc(String productCode);
    boolean existsByProductCodeAndRetailerCodeAndChecksum(String productCode, String retailerCode, String checksum);
    long countByProductCode(String productCode);
}
