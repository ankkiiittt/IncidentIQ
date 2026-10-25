package com.incidentiq.incident.dto;

import com.incidentiq.incident.entity.IncidentEvent;
import com.incidentiq.incident.entity.IncidentEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IncidentEventResponse(
        UUID id,
        UUID incidentId,
        IncidentEventType eventType,
        UUID actorId,
        String actorName,
        Map<String, Object> payload,
        Instant createdAt
) {
    public static IncidentEventResponse fromEntity(IncidentEvent event) {
        return new IncidentEventResponse(
                event.getId(),
                event.getIncident() != null ? event.getIncident().getId() : null,
                event.getEventType(),
                event.getActor() != null ? event.getActor().getId() : null,
                event.getActor() != null ? event.getActor().getFullName() : null,
                event.getPayload(),
                event.getCreatedAt()
        );
    }
}
