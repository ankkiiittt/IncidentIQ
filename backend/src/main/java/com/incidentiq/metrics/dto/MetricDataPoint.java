package com.incidentiq.metrics.dto;

import java.time.Instant;

public record MetricDataPoint(
        Instant timestamp,
        Double value
) {}
