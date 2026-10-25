package com.incidentiq.metrics.dto;

import com.incidentiq.metrics.entity.MetricType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record MetricIngestItem(
        @NotNull UUID serviceId,
        @NotNull MetricType metricName,
        @NotNull Double value,
        Instant recordedAt
) {}
