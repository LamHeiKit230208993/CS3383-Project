package com.pricewatch.notifications.persistence;

import com.pricewatch.notifications.WatchRepeat;
import com.pricewatch.notifications.WatchPriceKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "watch_rules")
public class WatchRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", length = 80, nullable = false)
    private String productCode;

    @Column(precision = 19, scale = 4)
    private BigDecimal target;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_kind", length = 16)
    private WatchPriceKind priceKind;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "next_due_at")
    private Instant nextDueAt;

    @Column(nullable = false)
    private boolean armed;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Column(name = "waiting_for_live", nullable = false)
    private boolean waitingForLive;

    @Column(name = "paused")
    private Boolean paused;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_frequency", length = 16)
    private WatchRepeat repeat;

    @Column(name = "schedule_start_at")
    private LocalDateTime startAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "price_anchor_at")
    private Instant priceAnchorAt;

    @Column(name = "next_price_at")
    private Instant nextPriceAt;

    @Column(name = "legacy_reminder_id", unique = true)
    private Long legacyReminderId;

    @Column(name = "cart_quantity")
    private Integer cartQuantity;

    @Column(name = "cart_retailer", length = 80)
    private String cartRetailer;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    protected WatchRuleEntity() {}

    public WatchRuleEntity(String productCode, BigDecimal target, Integer intervalDays, Instant now) {
        this.productCode = productCode;
        update(target, intervalDays, now);
    }

    public WatchRuleEntity(String productCode, BigDecimal target, LocalDateTime startAt, WatchRepeat repeat) {
        this.productCode = productCode;
        updateSchedule(target, startAt, repeat);
    }

    public Long getId() { return id; }
    public String getProductCode() { return productCode; }
    public BigDecimal getTarget() { return target; }
    public WatchPriceKind getPriceKind() { return priceKind == null ? WatchPriceKind.TARGET : priceKind; }
    public Integer getIntervalDays() { return intervalDays; }
    public Instant getNextDueAt() { return nextDueAt; }
    public boolean isArmed() { return armed; }
    public boolean isWaitingForLive() { return waitingForLive; }
    public boolean isEnabled() { return !Boolean.TRUE.equals(paused) && !isCompleted(); }
    public boolean isCompleted() { return completedAt != null; }
    public WatchRepeat getRepeat() { return repeat; }
    public LocalDateTime getStartAt() { return startAt; }
    public Integer getCartQuantity() { return cartQuantity; }
    public String getCartRetailer() { return cartRetailer; }
    public Instant getSnoozedUntil() { return snoozedUntil; }

    public void reviseCartReminder(int interval, int quantity, String retailer, Instant now) {
        cartQuantity = quantity;
        cartRetailer = retailer;
        if (!Objects.equals(intervalDays, interval)) {
            intervalDays = interval;
            advance(now);
        }
    }

    public void snoozeReminder(Instant until) {
        snoozedUntil = until;
    }

    public void setEnabled(boolean enabled) {
        if (enabled && isCompleted()) {
            throw new IllegalArgumentException("Edit or replace a completed one-time rule to schedule it again.");
        }
        paused = !enabled;
    }

    public void update(BigDecimal target, Integer intervalDays, Instant now) {
        if (this.target == null || this.target.compareTo(target) != 0) {
            this.target = target;
            armed = true;
        }
        if (!Objects.equals(this.intervalDays, intervalDays)) {
            this.intervalDays = intervalDays;
            nextDueAt = intervalDays == null ? null : now.plus(intervalDays, ChronoUnit.DAYS);
            armed = true;
            waitingForLive = false;
        }
    }

    public boolean matchesSchedule(LocalDateTime startAt, WatchRepeat repeat) {
        return this.repeat == repeat && Objects.equals(this.startAt, startAt);
    }

    public void updateSchedule(BigDecimal target, LocalDateTime startAt, WatchRepeat repeat) {
        updateSchedule(target, startAt, repeat, WatchPriceKind.TARGET);
    }

    public void updateSchedule(BigDecimal target, LocalDateTime startAt, WatchRepeat repeat, WatchPriceKind kind) {
        boolean targetChanged = !Objects.equals(this.target, target) || getPriceKind() != kind;
        boolean restarted = !matchesSchedule(startAt, repeat) || (isCompleted() && targetChanged);
        if (restarted) {
            intervalDays = null;
            nextDueAt = startAt == null ? null : WatchRepeat.inHongKong(startAt);
            completedAt = null;
            evaluatedAt = null;
            priceAnchorAt = null;
            nextPriceAt = null;
            waitingForLive = false;
            armed = true;
        }
        if (target == null) waitingForLive = false;
        this.target = target;
        this.priceKind = kind;
        this.startAt = startAt;
        this.repeat = repeat;
    }

    public boolean evaluatePrice(Instant retrievedAt, boolean qualifying) {
        if (evaluatedAt != null && retrievedAt.isBefore(evaluatedAt)) return false;
        evaluatedAt = retrievedAt;
        boolean shouldFire = armed && qualifying;
        armed = !qualifying;
        return shouldFire;
    }

    public boolean isDue(Instant now) {
        return nextDueAt != null && !nextDueAt.isAfter(now)
                && (snoozedUntil == null || !snoozedUntil.isAfter(now));
    }

    public boolean canFirePrice(Instant retrievedAt) {
        return (evaluatedAt == null || retrievedAt.isAfter(evaluatedAt))
                && (nextPriceAt == null || !retrievedAt.isBefore(nextPriceAt));
    }

    public void recordPrice(Instant retrievedAt, Instant now) {
        evaluatedAt = retrievedAt;
        if (repeat == WatchRepeat.ONCE) {
            completedAt = now;
        } else if (repeat == WatchRepeat.DAILY) {
            nextPriceAt = WatchRepeat.nextHongKongDay(retrievedAt);
        } else {
            if (priceAnchorAt == null) priceAnchorAt = now;
            nextPriceAt = repeat.nextAfter(priceAnchorAt, now);
        }
    }

    public void advance(Instant now) {
        snoozedUntil = null;
        if (repeat == null) {
            nextDueAt = now.plus(intervalDays, ChronoUnit.DAYS);
        } else {
            nextDueAt = repeat.nextAfter(startAt, now);
            if (repeat == WatchRepeat.ONCE) completedAt = now;
        }
        waitingForLive = false;
    }

    public void waitForLive() {
        waitingForLive = true;
    }
}
