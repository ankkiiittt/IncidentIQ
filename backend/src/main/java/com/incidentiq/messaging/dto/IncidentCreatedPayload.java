package com.incidentiq.messaging.dto;

import java.util.UUID;

public record IncidentCreatedPayload(
        UUID incidentId,
        UUID serviceId,
        String priority,
        String severity,
        UUID createdBy
) {}
