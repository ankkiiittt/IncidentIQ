package com.incidentiq.analytics.dto;

import com.incidentiq.incident.entity.IncidentCategory;

public record CategoryDistributionItem(
        IncidentCategory category,
        long count,
        double percentage
) {
}
