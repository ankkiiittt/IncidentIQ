package com.incidentiq.metrics.dto;

import java.time.Instant;
import java.util.UUID;

public record ServiceDeploymentItem(
        UUID id,
        String version,
        Instant deployedAt,
        String deployedBy,
        String notes
) {}
