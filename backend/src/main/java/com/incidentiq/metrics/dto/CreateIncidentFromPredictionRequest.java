package com.incidentiq.metrics.dto;

import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentSeverity;

public record CreateIncidentFromPredictionRequest(
        String title,
        String description,
        IncidentPriority priority,
        IncidentSeverity severity
) {}
