package com.incidentiq.metrics.dto;

import java.time.Instant;
import java.util.UUID;

public record ServiceIncidentItem(
        UUID id,
        Long incidentNumber,
        String title,
        String status,
        String priority,
        String severity,
        Instant createdAt
) {}
