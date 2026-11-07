package com.pricewatch.tracking;

import com.pricewatch.alerts.AlertService;
import com.pricewatch.catalog.ImportStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Runs after a successful catalogue import has already committed. It records
 * observations for tracked pairs and evaluates alert rules in its own work, so a
 * failure here does not roll back the accepted import (the plan's transaction
 * rule). A failed import never reaches this point, so it records nothing.
 */
@Service
public class PostImportProcessor {
    private final TrackingService tracking;
    private final AlertService alerts;

    public PostImportProcessor(TrackingService tracking, AlertService alerts) {
        this.tracking = tracking;
        this.alerts = alerts;
    }

    public int process(ImportStatus accepted) {
        Instant retrievedAt = accepted == null ? Instant.now() : accepted.retrievedAt();
        String checksum = accepted == null ? "" : accepted.checksum();
        int recorded = tracking.recordObservations(checksum, retrievedAt);
        int events = 0;
        for (String productCode : tracking.trackedProductCodes()) {
            events += alerts.evaluate(productCode);
        }
        return recorded + events;
    }
}
