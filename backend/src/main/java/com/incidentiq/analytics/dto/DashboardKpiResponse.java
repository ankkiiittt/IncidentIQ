package com.incidentiq.analytics.dto;

public record DashboardKpiResponse(
        long openP1,
        long openP2,
        long openP3,
        long openP4,
        long totalOpen,
        long unassigned,
        long slaBreached,
        long slaAtRisk,
        long resolvedLast7Days,
        Double mttrMinutes
) {}
