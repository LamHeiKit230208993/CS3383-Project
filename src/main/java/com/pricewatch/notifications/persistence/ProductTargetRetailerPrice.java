package com.pricewatch.notifications.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public record ProductTargetRetailerPrice(@Column(name = "retailer_code", length = 80, nullable = false) String retailerCode,
                                         @Column(precision = 19, scale = 4, nullable = false) BigDecimal price) {}
