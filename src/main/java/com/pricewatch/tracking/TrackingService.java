package com.pricewatch.tracking;

import com.pricewatch.catalog.importer.ImportedCatalog;
import com.pricewatch.catalog.persistence.AcceptedImportEntity;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.notifications.AlertService;
import com.pricewatch.notifications.repository.AlertEventRepository;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.persistence.SavedProductEntity;
import com.pricewatch.tracking.persistence.TrackingEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.SavedProductRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class TrackingService {
    private static final Duration HISTORY_RETENTION = Duration.ofDays(90);
    private static final List<String> PRICE_ALERT_TYPES = List.of("TARGET", "DROP");

    private final SavedProductRepository savedRepository;
    private final TrackingRepository trackingRepository;
    private final PriceObservationRepository observationRepository;
    private final AlertEventRepository alertEventRepository;
    private final ProductRepository productRepository;
    private final AcceptedImportRepository acceptedImportRepository;
    private final AlertService alertService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final Object startLock = new Object();

    public TrackingService(SavedProductRepository savedRepository,
                           TrackingRepository trackingRepository,
                           PriceObservationRepository observationRepository,
                           AlertEventRepository alertEventRepository,
                           ProductRepository productRepository,
                           AcceptedImportRepository acceptedImportRepository,
                           AlertService alertService,
                           Clock appClock,
                           PlatformTransactionManager transactionManager) {
        this.savedRepository = savedRepository;
        this.trackingRepository = trackingRepository;
        this.observationRepository = observationRepository;
        this.alertEventRepository = alertEventRepository;
        this.productRepository = productRepository;
        this.acceptedImportRepository = acceptedImportRepository;
        this.alertService = alertService;
        this.clock = appClock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public void save(String productCode) {
        ProductEntity product = currentProduct(productCode);
        if (!savedRepository.existsById(productCode)) {
            savedRepository.save(new SavedProductEntity(product, clock.instant()));
        }
    }

    @Transactional
    public void removeSaved(String productCode) {
        savedRepository.deleteById(productCode);
    }

    @Transactional
    public List<SavedProductView> savedProducts() {
        pruneExpired(clock.instant());
        return savedRepository.findAllByOrderBySavedAtDesc().stream()
                .map(entry -> new SavedProductView(
                        com.pricewatch.catalog.ProductView.from(entry.getProduct()), entry.getSavedAt()))
                .toList();
    }

    public TrackingView start(String productCode, String retailerCode) {
        synchronized (startLock) {
            TrackingView result = transactionTemplate.execute(status -> startInTransaction(productCode, retailerCode));
            return Objects.requireNonNull(result);
        }
    }

    private TrackingView startInTransaction(String productCode, String retailerCode) {
        if (retailerCode == null || retailerCode.isBlank()) {
            throw new IllegalArgumentException("Choose a retailer to track.");
        }
        ProductEntity product = currentProduct(productCode);
        boolean retailerExists = product.getListings().stream()
                .anyMatch(listing -> retailerCode.equals(listing.getRetailerCode()));
        if (!retailerExists) {
            throw new IllegalArgumentException("Choose a retailer currently listed for this product.");
        }

        TrackingEntity tracking = trackingRepository
                .findByProduct_CodeAndRetailerCodeAndEnabledTrue(productCode, retailerCode)
                .orElse(null);
        if (tracking == null) {
            Instant now = clock.instant();
            tracking = trackingRepository.saveAndFlush(new TrackingEntity(product, retailerCode, now));
            seedFromCurrentLiveImport(tracking, product);
        }
        return toView(tracking);
    }

    @Transactional
    public void stop(long trackingId) {
        TrackingEntity tracking = trackingRepository.findLockedById(trackingId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking period was not found."));
        if (tracking.isEnabled()) tracking.stop(clock.instant());
    }

    @Transactional
    public void deleteHistory(long trackingId) {
        TrackingEntity tracking = trackingRepository.findLockedById(trackingId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking period was not found."));
        observationRepository.deleteByTracking_Id(trackingId);
        alertEventRepository.deleteByTracking_IdAndTypeIn(trackingId, PRICE_ALERT_TYPES);
        tracking.setTargetArmed(true);
        tracking.clearPriceState();
    }

    @Transactional
    public void configureTarget(long trackingId, BigDecimal targetPrice) {
        TrackingEntity tracking = activeTracking(trackingId);
        BigDecimal normalized = normalizePrice(targetPrice);
        BigDecimal currentTarget = tracking.getTargetPrice();
        if (samePrice(currentTarget, normalized)) return;
        tracking.setTargetPrice(normalized);
        if (normalized != null) {
            Instant now = clock.instant();
            pruneExpired(now);
            currentLivePrice(tracking, now).ifPresent(current ->
                    alertService.evaluateCurrentTarget(tracking, current.price(), current.retrievedAt(), now));
        }
    }

    @Transactional
    public void configureDrop(long trackingId, boolean enabled) {
        TrackingEntity tracking = activeTracking(trackingId);
        tracking.setDropEnabled(enabled);
    }

    @Transactional
    public List<TrackingView> trackingPeriods() {
        pruneExpired(clock.instant());
        return trackingRepository.findAllByOrderByStartedAtDescIdDesc().stream().map(this::toView).toList();
    }

    /** Called from the catalogue replacement transaction, after the new listings have been stored. */
    @Transactional
    public void recordLiveImport(ImportedCatalog catalog, Instant retrievedAt, String sourceUrl, String checksum) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(HISTORY_RETENTION);
        observationRepository.deleteByObservedAtBefore(cutoff);
        if (retrievedAt.isBefore(cutoff) || retrievedAt.isAfter(now)) return;

        Map<String, Map<String, BigDecimal>> prices = new HashMap<>();
        for (ImportedCatalog.ImportedProduct product : catalog.products()) {
            Map<String, BigDecimal> byRetailer = new HashMap<>();
            product.listings().forEach(listing -> byRetailer.put(listing.retailerCode(), listing.price()));
            prices.put(product.code(), byRetailer);
        }

        for (TrackingEntity tracking : trackingRepository.findAllEnabledForUpdate()) {
            BigDecimal currentPrice = prices.getOrDefault(tracking.getProductCode(), Map.of())
                    .get(tracking.getRetailerCode());
            recordIfChanged(tracking, currentPrice, retrievedAt, sourceUrl, checksum);
        }
    }

    private ProductEntity currentProduct(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("A product is required.");
        }
        ProductEntity product = productRepository.findWithListingsByCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("That product is not in the accepted catalogue."));
        if (!product.isActive()) {
            throw new IllegalArgumentException("That product is no longer in the current catalogue.");
        }
        return product;
    }

    private TrackingEntity activeTracking(long trackingId) {
        TrackingEntity tracking = trackingRepository.findLockedById(trackingId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking period was not found."));
        if (!tracking.isEnabled()) {
            throw new IllegalArgumentException("Start a new tracking period before changing its alerts.");
        }
        return tracking;
    }

    private void seedFromCurrentLiveImport(TrackingEntity tracking, ProductEntity product) {
        AcceptedImportEntity accepted = acceptedImportRepository.findById(1L).orElse(null);
        if (accepted == null || !"LIVE".equals(accepted.getOrigin())) return;
        Instant now = clock.instant();
        if (accepted.getRetrievedAt().isBefore(now.minus(HISTORY_RETENTION))) return;
        var currentListing = product.getListings().stream()
                .filter(candidate -> tracking.getRetailerCode().equals(candidate.getRetailerCode()))
                .findFirst().orElse(null);
        if (currentListing == null) return;
        recordIfChanged(tracking, currentListing.getPrice(), accepted.getRetrievedAt(), accepted.getSourceUrl(),
                accepted.getChecksum());
    }

    private Optional<CurrentLivePrice> currentLivePrice(TrackingEntity tracking, Instant now) {
        AcceptedImportEntity accepted = acceptedImportRepository.findById(1L).orElse(null);
        if (accepted == null || !"LIVE".equals(accepted.getOrigin())
                || accepted.getRetrievedAt().isBefore(now.minus(HISTORY_RETENTION))) {
            return Optional.empty();
        }

        var listing = tracking.getProduct().getListings().stream()
                .filter(candidate -> tracking.getRetailerCode().equals(candidate.getRetailerCode()))
                .findFirst().orElse(null);
        if (listing == null || listing.getPrice() == null) return Optional.empty();

        return Optional.of(new CurrentLivePrice(listing.getPrice(), accepted.getRetrievedAt()));
    }

    private void recordIfChanged(TrackingEntity tracking, BigDecimal price, Instant observedAt,
                                 String sourceUrl, String checksum) {
        PriceObservationEntity previous = observationRepository
                .findTopByTracking_IdOrderByObservedAtDescIdDesc(tracking.getId()).orElse(null);
        if (tracking.hasObservedPriceState() && samePrice(tracking.getLastObservedPrice(), price)) {
            alertService.evaluateCurrentTarget(tracking, price, observedAt, clock.instant());
            return;
        }

        PriceObservationEntity observation = observationRepository.saveAndFlush(
                new PriceObservationEntity(tracking, price, observedAt, sourceUrl, checksum));
        tracking.recordPriceState(price, observedAt);
        alertService.evaluateNewObservation(tracking, observation, previous, clock.instant());
    }

    private void pruneExpired(Instant now) {
        observationRepository.deleteByObservedAtBefore(now.minus(HISTORY_RETENTION));
    }

    private TrackingView toView(TrackingEntity tracking) {
        List<PriceObservationView> history = observationRepository
                .findByTracking_IdOrderByObservedAtDescIdDesc(tracking.getId()).stream()
                .map(observation -> new PriceObservationView(observation.getPrice(), observation.getObservedAt(),
                        observation.getSourceUrl()))
                .toList();
        return new TrackingView(tracking.getId(), tracking.getProductCode(),
                tracking.getProduct().getName() == null ? tracking.getProductCode()
                        : tracking.getProduct().getName().getEn(),
                tracking.getRetailerCode(), tracking.isEnabled(), tracking.getStartedAt(), tracking.getStoppedAt(),
                tracking.getTargetPrice(), tracking.isDropEnabled(), history);
    }

    private static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) return null;
        BigDecimal normalized;
        try {
            normalized = price.setScale(4, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Use a price from 0 to 999999999999999.9999.");
        }
        if (normalized.signum() < 0 || normalized.precision() > 19) {
            throw new IllegalArgumentException("Use a price from 0 to 999999999999999.9999.");
        }
        return normalized;
    }

    private static boolean samePrice(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private record CurrentLivePrice(BigDecimal price, Instant retrievedAt) {
    }
}
