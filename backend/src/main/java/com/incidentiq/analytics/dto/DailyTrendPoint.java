package com.incidentiq.analytics.dto;

public record DailyTrendPoint(
        String date,
        long createdCount,
        long resolvedCount
) {}
