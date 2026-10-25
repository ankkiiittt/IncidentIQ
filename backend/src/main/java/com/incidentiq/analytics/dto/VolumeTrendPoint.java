package com.incidentiq.analytics.dto;

public record VolumeTrendPoint(
        String timestamp,
        long p1,
        long p2,
        long p3,
        long p4,
        long total
) {
}
