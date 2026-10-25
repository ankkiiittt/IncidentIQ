package com.incidentiq.analytics.dto;

import java.util.UUID;

public interface TopAffectedServiceProjection {
    UUID getServiceId();
    String getServiceName();
    String getServiceTier();
    long getActiveIncidents();
    long getTotalIncidents();
}
