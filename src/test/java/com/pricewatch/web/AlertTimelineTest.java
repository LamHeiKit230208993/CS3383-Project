package com.pricewatch.web;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertTimelineTest {
    @Test
    void midnightInHongKongStartsANewDayAndKeepsNewestFirst() {
        var midnight = entry("2026-11-20T16:00:00Z");
        var beforeMidnight = entry("2026-11-20T15:59:00Z");
        var earlier = entry("2026-11-20T00:00:00Z");

        var days = AlertCenterController.timelineDays(
                List.of(midnight, beforeMidnight, earlier), Locale.UK);

        assertEquals(2, days.size());
        assertEquals(LocalDate.of(2026, 11, 21), days.getFirst().date());
        assertEquals(List.of(midnight), days.getFirst().entries());
        assertEquals("00:00", midnight.clockTime());
        assertEquals(LocalDate.of(2026, 11, 20), days.getLast().date());
        assertEquals(List.of(beforeMidnight, earlier), days.getLast().entries());
        assertEquals("23:59", beforeMidnight.clockTime());
    }

    @Test
    void groupsOnlyTheSameProductAndExactTriggerInstant() {
        var first = entry("2026-11-21T12:30:42Z");
        var sameTrigger = entry("2026-11-21T12:30:42Z");
        var differentProduct = new AlertCenterController.TimelineEntry(
                first.createdAt(), "P000000125", null, null);
        var differentSecond = entry("2026-11-21T12:30:41Z");

        var group = AlertCenterController.timelineDays(
                List.of(first, differentProduct, sameTrigger, differentSecond), Locale.UK).getFirst().groups();

        assertEquals(3, group.size());
        assertEquals(List.of(first, sameTrigger), group.getFirst().entries());
        assertEquals(List.of(differentProduct), group.get(1).entries());
        assertEquals(List.of(differentSecond), group.getLast().entries());
    }

    private static AlertCenterController.TimelineEntry entry(String createdAt) {
        return new AlertCenterController.TimelineEntry(Instant.parse(createdAt), "P000000124", null, null);
    }
}
