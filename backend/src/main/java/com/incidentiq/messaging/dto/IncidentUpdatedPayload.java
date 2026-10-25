package com.incidentiq.messaging.dto;

import java.util.UUID;

public record IncidentUpdatedPayload(
        UUID incidentId,
        String status,
        String priority,
        UUID actorId,
        String updateType
) {}
