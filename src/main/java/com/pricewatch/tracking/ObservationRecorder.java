package com.pricewatch.tracking;

import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.persistence.RetailerListingEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.persistence.TrackingChoiceEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingChoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Records price observations for tracked (product, retailer) pairs after a successful
 * catalogue import (D03: one observation per pair per import; unknown prices skipped).
 * This runs in its own transaction. A failure here never rolls back an already
 * accepted catalogue.
 */
@Service
public class ObservationRecorder {
    private static final Logger log = LoggerFactory.getLogger(ObservationRecorder.class);

    private final TrackingChoiceRepository trackingRepository;
    private final PriceObservationRepository observationRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;

    public ObservationRecorder(TrackingChoiceRepository trackingRepository,
                               PriceObservationRepository observationRepository,
                               ProductRepository productRepository,
                               PlatformTransactionManager transactionManager) {
        this.trackingRepository = trackingRepository;
        this.observationRepository = observationRepository;
        this.productRepository = productRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * @return an error message if recording failed, otherwise null. The accepted
     *         import stays committed either way.
     */
    public String recordAfterImport(Instant retrievedAt, String checksum) {
        try {
            transactionTemplate.executeWithoutResult(status -> record(retrievedAt, checksum));
            return null;
        } catch (RuntimeException exception) {
            log.warn("Recording tracked price observations failed; the accepted import was kept", exception);
            return "Price history update failed after the import. Your accepted catalogue was kept; history will retry on the next import.";
        }
    }

    private void record(Instant retrievedAt, String checksum) {
        List<TrackingChoiceEntity> enabled = trackingRepository.findAllByEnabledTrue();
        List<PriceObservationEntity> toSave = new ArrayList<>();
        for (TrackingChoiceEntity choice : enabled) {
            ProductEntity product = productRepository.findWithListingsByCode(choice.getProductCode())
                    .orElse(null);
            if (product == null) {
                continue;
            }
            RetailerListingEntity listing = product.getListings().stream()
                    .filter(candidate -> candidate.getRetailerCode().equals(choice.getRetailerCode()))
                    .findFirst().orElse(null);
            if (listing == null || listing.getPrice() == null) {
                continue;
            }
            if (observationRepository.existsByProductCodeAndRetailerCodeAndChecksum(
                    choice.getProductCode(), choice.getRetailerCode(), checksum)) {
                continue;
            }
            toSave.add(new PriceObservationEntity(
                    choice.getProductCode(), choice.getRetailerCode(), listing.getPrice(),
                    retrievedAt, checksum, offerSnapshot(listing)));
        }
        if (!toSave.isEmpty()) {
            observationRepository.saveAll(toSave);
        }
    }

    private static String offerSnapshot(RetailerListingEntity listing) {
        return listing.getOffers().stream()
                .map(offer -> {
                    var text = offer.getText();
                    if (text == null) {
                        return "";
                    }
                    return String.join(" | ",
                            nullToEmpty(text.getEn()), nullToEmpty(text.getZhHant()),
                            nullToEmpty(text.getZhHans()));
                })
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + " ;; " + right)
                .map(value -> value.length() > 2000 ? value.substring(0, 2000) : value)
                .orElse(null);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
