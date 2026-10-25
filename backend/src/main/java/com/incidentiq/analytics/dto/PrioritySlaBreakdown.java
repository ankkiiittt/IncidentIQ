package com.incidentiq.analytics.dto;

import com.incidentiq.incident.entity.IncidentPriority;

public record PrioritySlaBreakdown(
        IncidentPriority priority,
        int targetMinutes,
        long metCount,
        long breachedCount,
        double complianceRate
) {
}
