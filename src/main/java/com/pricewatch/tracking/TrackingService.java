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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class TrackingService {
    private final WishlistEntryRepository wishlistRepository;
    private final TrackingChoiceRepository trackingRepository;
    private final PriceObservationRepository observationRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;
    private final Object mutationLock = new Object();
    private final Clock clock;

    public TrackingService(WishlistEntryRepository wishlistRepository,
                           TrackingChoiceRepository trackingRepository,
                           PriceObservationRepository observationRepository,
                           ProductRepository productRepository,
                           PlatformTransactionManager transactionManager,
                           Clock clock) {
        this.wishlistRepository = wishlistRepository;
        this.trackingRepository = trackingRepository;
        this.observationRepository = observationRepository;
        this.productRepository = productRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean isSaved(String productCode) {
        return wishlistRepository.existsById(productCode);
    }

    @Transactional(readOnly = true)
    public boolean isTracked(String productCode) {
        return trackingRepository.findAllByProductCode(productCode).stream()
                .anyMatch(TrackingChoiceEntity::isEnabled);
    }

    public void save(String productCode) {
        mutate(() -> {
            if (!wishlistRepository.existsById(productCode)) {
                wishlistRepository.save(new WishlistEntryEntity(productCode, clock.instant()));
            }
        });
    }

    public void unsave(String productCode) {
        mutate(() -> wishlistRepository.deleteById(productCode));
    }

    /**
     * Enable tracking for every priced retailer listing of the product (D01).
     * Tracking is not seeded automatically; the caller must choose to enable it.
     */
    public void enableTracking(String productCode) {
        mutate(() -> {
            ProductEntity product = productRepository.findWithListingsByCode(productCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "That product is not in the accepted catalogue."));
            for (RetailerListingEntity listing : product.getListings()) {
                if (listing.getPrice() == null) {
                    continue;
                }
                TrackingChoiceEntity choice = trackingRepository
                        .findByProductCodeAndRetailerCode(productCode, listing.getRetailerCode())
                        .orElseGet(() -> new TrackingChoiceEntity(
                                productCode, listing.getRetailerCode(), clock.instant()));
                choice.setEnabled(true);
                trackingRepository.save(choice);
            }
        });
    }

    public void disableTracking(String productCode) {
        mutate(() -> trackingRepository.findAllByProductCode(productCode)
                .forEach(choice -> {
                    choice.setEnabled(false);
                    trackingRepository.save(choice);
                }));
    }

    @Transactional(readOnly = true)
    public List<TrackedProductView> savedProducts() {
        List<TrackedProductView> views = new ArrayList<>();
        for (WishlistEntryEntity entry : wishlistRepository.findAll()) {
            views.add(toView(entry.getProductCode()));
        }
        return views;
    }

    @Transactional(readOnly = true)
    public List<TrackedProductView> trackedProducts() {
        Set<String> codes = new LinkedHashSet<>();
        trackingRepository.findAllByEnabledTrue()
                .forEach(choice -> codes.add(choice.getProductCode()));
        List<TrackedProductView> views = new ArrayList<>();
        for (String code : codes) {
            views.add(toView(code));
        }
        return views;
    }

    private TrackedProductView toView(String productCode) {
        Optional<ProductEntity> product = productRepository.findWithListingsByCode(productCode);
        List<TrackingChoiceEntity> choices = trackingRepository.findAllByProductCode(productCode);
        List<String> retailers = choices.stream()
                .filter(TrackingChoiceEntity::isEnabled)
                .map(TrackingChoiceEntity::getRetailerCode)
                .sorted()
                .toList();
        boolean tracked = !retailers.isEmpty();
        List<TrackedProductView.ObservationView> observations = observationRepository
                .findAllByProductCodeOrderByRetrievedAtDescIdDesc(productCode).stream()
                .map(observation -> new TrackedProductView.ObservationView(
                        observation.getRetailerCode(), observation.getPrice(),
                        observation.getRetrievedAt(), observation.getOfferSnapshot()))
                .toList();
        return new TrackedProductView(
                productCode,
                product.map(p -> label(p.getName() == null ? null : p.getName().getEn())).orElse(""),
                product.map(p -> label(p.getBrand() == null ? null : p.getBrand().getEn())).orElse(""),
                product.map(ProductEntity::isActive).orElse(false),
                wishlistRepository.existsById(productCode),
                tracked,
                retailers,
                observations);
    }

    private static String label(String value) {
        return value == null ? "" : value;
    }

    private void mutate(Runnable operation) {
        synchronized (mutationLock) {
            transactionTemplate.executeWithoutResult(status -> operation.run());
        }
    }

    /** Codes of products with at least one retailer being tracked. */
    @Transactional(readOnly = true)
    public Set<String> trackedProductCodes() {
        return Stream.of(trackingRepository.findAllByEnabledTrue())
                .flatMap(List::stream)
                .map(TrackingChoiceEntity::getProductCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
