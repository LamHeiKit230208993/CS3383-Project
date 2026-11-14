package com.pricewatch.notifications.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "notification_settings")
public class NotificationSettingsEntity {
    @Id
    private Long id = 1L;

    @Column(name = "quiet_start", nullable = false)
    private LocalTime quietStart;

    @Column(name = "quiet_end", nullable = false)
    private LocalTime quietEnd;

    @Column(name = "summary_time", nullable = false)
    private LocalTime summaryTime;

    protected NotificationSettingsEntity() {
    }

    public NotificationSettingsEntity(LocalTime quietStart, LocalTime quietEnd, LocalTime summaryTime) {
        this.quietStart = quietStart;
        this.quietEnd = quietEnd;
        this.summaryTime = summaryTime;
    }

    public LocalTime getQuietStart() { return quietStart; }
    public LocalTime getQuietEnd() { return quietEnd; }
    public LocalTime getSummaryTime() { return summaryTime; }

    public void setTimes(LocalTime quietStart, LocalTime quietEnd, LocalTime summaryTime) {
        this.quietStart = quietStart;
        this.quietEnd = quietEnd;
        this.summaryTime = summaryTime;
    }
}
