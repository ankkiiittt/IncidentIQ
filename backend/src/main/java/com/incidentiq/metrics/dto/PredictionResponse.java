package com.incidentiq.metrics.dto;

import com.incidentiq.metrics.entity.Prediction;
import com.incidentiq.metrics.entity.PredictionRiskLevel;
import com.incidentiq.metrics.entity.PredictionSource;
import com.incidentiq.metrics.entity.PredictionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PredictionResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        BigDecimal anomalyScore,
        PredictionRiskLevel riskLevel,
        BigDecimal failureProbability,
        Integer horizonMinutes,
        PredictionSource source,
        String modelVersion,
        Map<String, Object> contributingMetrics,
        PredictionStatus status,
        UUID incidentId,
        Instant createdAt
) {
    public static PredictionResponse fromEntity(Prediction p) {
        return new PredictionResponse(
                p.getId(),
                p.getService().getId(),
                p.getService().getName(),
                p.getAnomalyScore(),
                p.getRiskLevel(),
                p.getFailureProbability(),
                p.getHorizonMinutes(),
                p.getSource(),
                p.getModelVersion(),
                p.getContributingMetrics(),
                p.getStatus(),
                p.getIncident() != null ? p.getIncident().getId() : null,
                p.getCreatedAt()
        );
    }
}
