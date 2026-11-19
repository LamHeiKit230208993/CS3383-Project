package com.pricewatch.tracking;

import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.persistence.LocalizedLabel;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.i18n.AppMessages;
import com.pricewatch.tracking.persistence.PriceObservationEntity;
import com.pricewatch.tracking.persistence.TrackingEntity;
import com.pricewatch.tracking.repository.PriceObservationRepository;
import com.pricewatch.tracking.repository.TrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Product-level history for tracked products. Only accepted live observations
 * inside the retention window are shown; long intervals become explicit gaps
 * instead of invented daily values.
 */
@Service
public class ProductHistoryService {
    static final int RETENTION_DAYS = 90;
    static final int GAP_DAYS = 7;

    private final TrackingRepository trackingRepository;
    private final PriceObservationRepository observationRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    public ProductHistoryService(TrackingRepository trackingRepository,
                                 PriceObservationRepository observationRepository,
                                 ProductRepository productRepository,
                                 Clock clock) {
        this.trackingRepository = trackingRepository;
        this.observationRepository = observationRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProductHistoryView history(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException(AppMessages.text("error.product.codeRequired"));
        }
        ProductEntity product = productRepository.findById(productCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        AppMessages.text("error.product.notFound", productCode)));
        List<TrackingEntity> periods = trackingRepository
                .findByProduct_CodeOrderByStartedAtAscIdAsc(productCode);
        if (periods.isEmpty()) {
            throw new IllegalArgumentException(AppMessages.text("error.history.notTracked"));
        }

        Instant cutoff = clock.instant().minus(Duration.ofDays(RETENTION_DAYS));
        List<RetailerHistoryView> views = periods.stream()
                .map(period -> toView(period, cutoff))
                .toList();
        LocalizedText name = text(product.getName());
        if (name.textFor(java.util.Locale.ENGLISH).isBlank()) {
            name = new LocalizedText(product.getCode(), product.getCode(), product.getCode());
        }
        return new ProductHistoryView(product.getCode(), name, text(product.getBrand()),
                product.isActive(), views, RETENTION_DAYS, GAP_DAYS);
    }

    private RetailerHistoryView toView(TrackingEntity period, Instant cutoff) {
        List<PriceObservationEntity> observations = observationRepository
                .findByTracking_IdOrderByObservedAtAscIdAsc(period.getId()).stream()
                .filter(observation -> !observation.getObservedAt().isBefore(cutoff))
                .filter(observation -> !observation.getObservedAt().isAfter(clock.instant()))
                .toList();

        List<HistoryEntryView> entries = new ArrayList<>(observations.size());
        Instant previous = null;
        for (PriceObservationEntity observation : observations) {
            if (previous != null) {
                long days = Duration.between(previous, observation.getObservedAt()).toDays();
                if (days >= GAP_DAYS) entries.add(HistoryEntryView.gap(days));
            }
            entries.add(HistoryEntryView.observation(observation.getPrice(), observation.getObservedAt()));
            previous = observation.getObservedAt();
        }

        return new RetailerHistoryView(period.getId(), period.getRetailerCode(), period.isEnabled(),
                period.getStartedAt(), period.getStoppedAt(), period.getTargetPrice(), period.isDropEnabled(),
                entries);
    }

    private static LocalizedText text(LocalizedLabel label) {
        return label == null ? new LocalizedText("", "", "")
                : new LocalizedText(label.getEn(), label.getZhHant(), label.getZhHans());
    }
}
