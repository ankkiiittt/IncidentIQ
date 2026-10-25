package com.incidentiq.catalog.dto;

import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.entity.ServiceEnvironment;

import java.time.Instant;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        String name,
        ServiceEnvironment environment,
        String ownerTeam,
        ServiceCriticality criticality,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static ServiceResponse fromEntity(ServiceEntity entity) {
        return new ServiceResponse(
                entity.getId(),
                entity.getName(),
                entity.getEnvironment(),
                entity.getOwnerTeam(),
                entity.getCriticality(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
