package com.incidentiq.metrics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MetricBatchRequest(
        @NotEmpty @Valid List<MetricIngestItem> metrics
) {}
