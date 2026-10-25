package com.incidentiq.analytics.dto;

import java.util.UUID;

public record TopAffectedServiceDto(
        UUID serviceId,
        String serviceName,
        String serviceTier,
        long activeIncidents,
        long totalIncidents
) {}
