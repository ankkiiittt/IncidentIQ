package com.incidentiq.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String type,
        int schemaVersion,
        Instant occurredAt,
        UUID aggregateId,
        String correlationId,
        T payload
) {
    public static <T> EventEnvelope<T> of(String type, UUID aggregateId, String correlationId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID(),
                type,
                1,
                Instant.now(),
                aggregateId,
                correlationId,
                payload
        );
    }
}
