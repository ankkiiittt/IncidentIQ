package com.incidentiq.analytics.dto;

import com.incidentiq.incident.dto.IncidentSummaryResponse;

import java.util.List;

public record DashboardResponse(
        DashboardKpiResponse kpis,
        List<DailyTrendPoint> trend30Days,
        List<TopAffectedServiceDto> topAffectedServices,
        List<IncidentSummaryResponse> recentIncidents,
        ServiceHealthSummaryDto serviceHealth
) {}
