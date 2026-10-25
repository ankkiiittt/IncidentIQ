package com.incidentiq.analytics.dto;

import java.util.List;

public record AnalyticsCausesResponse(
        int days,
        List<CategoryDistributionItem> categoryDistribution,
        List<RecurringCauseItem> recurringCauses
) {
}
