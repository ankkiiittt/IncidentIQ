package com.incidentiq.metrics.dto;

import com.incidentiq.metrics.entity.PredictionRiskLevel;
import java.util.List;
import java.util.UUID;

public record ServiceHealthDetailResponse(
        UUID serviceId,
        String serviceName,
        String environment,
        String ownerTeam,
        String criticality,
        String healthStatus, // HEALTHY, DEGRADED, CRITICAL
        PredictionRiskLevel riskLevel, // LOW, MEDIUM, HIGH
        List<MetricSeries> metricSeries,
        List<PredictionResponse> activePredictions,
        List<ServiceDeploymentItem> recentDeployments,
        List<ServiceIncidentItem> openIncidents
) {}
