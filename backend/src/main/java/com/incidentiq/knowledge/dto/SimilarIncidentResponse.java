package com.incidentiq.knowledge.dto;

import java.time.Instant;
import java.util.UUID;

public record SimilarIncidentResponse(
        UUID documentId,
        UUID incidentId,
        String formattedNumber,
        String title,
        String severity,
        String priority,
        String status,
        String rootCause,
        String resolution,
        Instant resolvedAt,
        String serviceName,
        double similarity
) {}
