package com.incidentiq.analytics.dto;

import java.util.List;

public record AnalyticsSlaResponse(
        int days,
        double overallComplianceRate,
        List<PrioritySlaBreakdown> byPriority
) {
}
