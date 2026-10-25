package com.incidentiq.analytics.dto;

import java.time.Instant;
import java.util.UUID;

public interface RecurringCauseProjection {
    UUID getServiceId();
    String getServiceName();
    String getCategory();
    long getIncidentCount();
    Long getLastIncidentNumber();
    Instant getLastOccurredAt();
}
