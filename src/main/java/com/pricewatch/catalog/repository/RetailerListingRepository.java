package com.pricewatch.catalog.repository;

import com.pricewatch.catalog.persistence.RetailerListingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface RetailerListingRepository extends JpaRepository<RetailerListingEntity, Long> {
    @Query("""
            select listing.product.code as productCode, listing.retailerCode as retailerCode,
                listing.price as price, accepted.retrievedAt as retrievedAt
            from RetailerListingEntity listing, AcceptedImportEntity accepted
            where accepted.id = 1 and accepted.origin = 'LIVE' and listing.price is not null
                and listing.product.code in :productCodes
            order by listing.retailerCode
            """)
    List<LiveListing> findAcceptedLivePrices(Collection<String> productCodes);

    interface LiveListing {
        String getProductCode();
        String getRetailerCode();
        BigDecimal getPrice();
        Instant getRetrievedAt();
    }
}
