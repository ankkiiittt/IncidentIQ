package com.incidentiq.incident.dto;

import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentSeverity;
import com.incidentiq.incident.entity.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentSearchCriteria(
        String q,
        IncidentStatus status,
        IncidentPriority priority,
        IncidentSeverity severity,
        IncidentCategory category,
        UUID serviceId,
        UUID assigneeId,
        String tag,
        Instant createdAfter,
        Instant createdBefore
) {}
