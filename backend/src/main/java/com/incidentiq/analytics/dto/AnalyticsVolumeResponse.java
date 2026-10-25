package com.incidentiq.analytics.dto;

import java.util.List;

public record AnalyticsVolumeResponse(
        int days,
        String groupBy,
        List<VolumeTrendPoint> series
) {
}
