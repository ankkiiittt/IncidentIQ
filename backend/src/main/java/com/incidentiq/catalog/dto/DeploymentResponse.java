package com.incidentiq.catalog.dto;

import com.incidentiq.catalog.entity.DeploymentEntity;
import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        String version,
        Instant deployedAt,
        String deployedBy,
        String notes,
        Instant createdAt
) {
    public static DeploymentResponse fromEntity(DeploymentEntity entity) {
        return new DeploymentResponse(
                entity.getId(),
                entity.getService().getId(),
                entity.getService().getName(),
                entity.getVersion(),
                entity.getDeployedAt(),
                entity.getDeployedBy(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }
}
