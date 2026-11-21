package com.pricewatch.notifications;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record WatchRuleView(long id, String productCode, BigDecimal target,
                            Integer intervalDays, Instant nextDueAt, boolean armed, boolean waitingForLive,
                            boolean enabled, String repeat, LocalDateTime startAt, boolean completed) {}
