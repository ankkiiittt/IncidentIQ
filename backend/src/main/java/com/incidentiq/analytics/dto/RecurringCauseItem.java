package com.incidentiq.analytics.dto;

import com.incidentiq.incident.entity.IncidentCategory;

import java.time.Instant;
import java.util.UUID;

public record RecurringCauseItem(
        UUID serviceId,
        String serviceName,
        IncidentCategory category,
        long incidentCount,
        Long lastIncidentNumber,
        Instant lastOccurredAt,
        boolean isRecurring
) {
}
