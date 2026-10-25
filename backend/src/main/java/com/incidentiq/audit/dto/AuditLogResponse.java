package com.incidentiq.audit.dto;

import com.incidentiq.audit.entity.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        Long id,
        UUID userId,
        String action,
        String entityType,
        String entityId,
        String metadata,
        String ipAddress,
        String correlationId,
        Instant createdAt
) {
    public static AuditLogResponse fromEntity(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getMetadata(),
                log.getIpAddress(),
                log.getCorrelationId(),
                log.getCreatedAt()
        );
    }
}
