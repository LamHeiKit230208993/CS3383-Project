package com.pricewatch.catalog.event;

import com.pricewatch.catalog.ImportStatus;

/**
 * Published inside the catalogue replacement transaction. Listeners must use
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} so that a rolled-back
 * import never produces history, alerts, or other derived records.
 */
public record CatalogueReplacedEvent(ImportStatus status) {
}
