package com.incidentiq.analytics.dto;

public interface DailyIncidentTrendProjection {
    String getDay();
    long getCreatedCount();
    long getResolvedCount();
}
