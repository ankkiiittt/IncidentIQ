package com.incidentiq.analytics.dto;

import com.incidentiq.incident.entity.IncidentPriority;

import java.util.Map;

public record AnalyticsOverviewResponse(
        int days,
        double mttrMinutes,
        double mttdMinutes,
        double slaComplianceRate,
        long totalCreated,
        long totalResolved,
        long activeCount,
        Map<IncidentPriority, Long> priorityBreakdown
) {
}
