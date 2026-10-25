package com.incidentiq.metrics.dto;

public record MetricBatchResponse(
        int totalSubmitted,
        int inserted,
        int duplicatesSkipped,
        int anomaliesDetected
) {}
