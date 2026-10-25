package com.incidentiq.analytics.dto;

public record AiFeedbackMetricsResponse(
        int days,
        long totalAnalyses,
        long helpfulCount,
        long notHelpfulCount,
        double helpfulRate,
        long acceptedCount,
        long rejectedCount,
        double acceptanceRate,
        double avgConfidence,
        double avgLatencyMs
) {
}
