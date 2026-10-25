package com.incidentiq.messaging.dto;

import java.util.UUID;

public record IncidentEscalatedPayload(
        UUID incidentId,
        String fromPriority,
        String toPriority,
        UUID actorId,
        String reason
) {}
