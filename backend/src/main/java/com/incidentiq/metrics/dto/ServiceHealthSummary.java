package com.incidentiq.metrics.dto;

import com.incidentiq.metrics.entity.PredictionRiskLevel;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ServiceHealthSummary(
        UUID serviceId,
        String serviceName,
        String environment,
        String ownerTeam,
        String criticality,
        String healthStatus, // HEALTHY, DEGRADED, CRITICAL
        PredictionRiskLevel riskLevel, // LOW, MEDIUM, HIGH
        long openIncidentCount,
        long activePredictionCount,
        Map<String, Double> latestMetrics,
        Instant lastMetricRecordedAt
) {}
