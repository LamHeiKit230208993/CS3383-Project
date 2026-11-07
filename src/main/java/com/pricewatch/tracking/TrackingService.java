package com.pricewatch.tracking;

import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.persistence.RetailerListingEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.persistence.TrackingChoiceEntity;
import com.pricewatch.tracking.persistence.WishlistEntryEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingChoiceRepository;
import com.pricewatch.tracking.repository.WishlistEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * J05: wishlist (save) is separate from explicit tracking. Tracking a product
 * seeds one enabled choice per retailer currently listed (D01). Observations are
 * recorded only after a successful import, in a separate transaction from the
 * catalogue commit (see {@link #recordObservations}). Unknown prices are recorded
 * for provenance only and are excluded from later comparisons (D03).
 */
@Service
public class TrackingService {
    private final WishlistEntryRepository wishlist;
    private final TrackingChoiceRepository tracking;
    private final PriceObservationRepository observations;
    private final ProductRepository products;
    private final Clock clock;

    public TrackingService(WishlistEntryRepository wishlist,
                           TrackingChoiceRepository tracking,
                           PriceObservationRepository observations,
                           ProductRepository products,
                           Clock clock) {
        this.wishlist = wishlist;
        this.tracking = tracking;
        this.observations = observations;
        this.products = products;
        this.clock = clock;
    }

    // --- Wishlist (save only; never starts tracking) ---

    @Transactional
    public void save(String productCode) {
        if (!products.existsById(productCode)) {
            throw new IllegalArgumentException("That product is not in the accepted catalogue.");
        }
        if (!wishlist.existsById(productCode)) {
            wishlist.save(new WishlistEntryEntity(productCode, clock.instant()));
        }
    }

    @Transactional
    public void unsave(String productCode) {
        wishlist.deleteById(productCode);
    }

    @Transactional(readOnly = true)
    public List<WishlistEntryEntity> wishlistEntries() {
        return wishlist.findAllByOrderBySavedAtDesc();
    }

    @Transactional(readOnly = true)
    public boolean isSaved(String productCode) {
        return wishlist.existsById(productCode);
    }

    // --- Tracking ---

    @Transactional(readOnly = true)
    public boolean isTracked(String productCode) {
        return tracking.findAllByProductCode(productCode).stream().anyMatch(TrackingChoiceEntity::isEnabled);
    }

    @Transactional(readOnly = true)
    public List<TrackingChoiceEntity> activeChoices() {
        return tracking.findAllByEnabledTrueOrderByProductCodeAscRetailerCodeAsc();
    }

    @Transactional(readOnly = true)
    public List<String> trackedProductCodes() {
        return tracking.findAllByEnabledTrueOrderByProductCodeAscRetailerCodeAsc().stream()
                .map(TrackingChoiceEntity::getProductCode)
                .distinct()
                .toList();
    }

    @Transactional
    public void enableTracking(String productCode) {
        ProductEntity product = products.findWithListingsByCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("That product is not in the accepted catalogue."));
        if (!product.isActive()) {
            throw new IllegalArgumentException("That product is no longer in the current catalogue.");
        }
        Instant now = clock.instant();
        for (RetailerListingEntity listing : product.getListings()) {
            String retailer = listing.getRetailerCode();
            TrackingChoiceEntity choice = tracking.findByProductCodeAndRetailerCode(productCode, retailer).orElse(null);
            long nextPeriod = choice == null ? 1 : choice.getPeriodId() + 1;
            if (choice == null) {
                tracking.save(new TrackingChoiceEntity(productCode, retailer, nextPeriod, now));
            } else if (!choice.isEnabled()) {
                choice.enable(nextPeriod, now);
            }
        }
    }

    @Transactional
    public void disableTracking(String productCode) {
        Instant now = clock.instant();
        tracking.findAllByProductCode(productCode).stream()
                .filter(TrackingChoiceEntity::isEnabled)
                .forEach(choice -> choice.disable(now));
    }

    /**
     * Record observations for every enabled tracked pair present in the accepted
     * catalogue. Called after a successful import so a failed import adds nothing.
     * Duplicate identity (product, retailer, checksum, period) is skipped (D03).
     */
    @Transactional
    public int recordObservations(String checksum, Instant retrievedAt) {
        int recorded = 0;
        for (TrackingChoiceEntity choice : tracking.findAllByEnabledTrue()) {
            Optional<ProductEntity> product = products.findWithListingsByCode(choice.getProductCode());
            if (product.isEmpty() || !product.get().isActive()) {
                continue;
            }
            Optional<RetailerListingEntity> listing = product.get().getListings().stream()
                    .filter(l -> l.getRetailerCode().equals(choice.getRetailerCode()))
                    .findFirst();
            if (listing.isEmpty()) {
                continue;
            }
            if (observations.existsByProductCodeAndRetailerCodeAndChecksumAndPeriodId(
                    choice.getProductCode(), choice.getRetailerCode(), checksum, choice.getPeriodId())) {
                continue;
            }
            observations.save(new PriceObservationEntity(
                    choice.getProductCode(), choice.getRetailerCode(), choice.getPeriodId(),
                    listing.get().getPrice(), checksum, retrievedAt, null));
            recorded++;
        }
        return recorded;
    }

    @Transactional(readOnly = true)
    public List<PriceObservationEntity> history(String productCode, String retailerCode) {
        return observations.findAllByProductCodeAndRetailerCodeOrderByRetrievedAtAscIdAsc(productCode, retailerCode);
    }
}
