package com.incidentiq.catalog.dto;

import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceRequest(
        @NotBlank(message = "Service name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotNull(message = "Environment is required")
        ServiceEnvironment environment,

        @NotBlank(message = "Owner team is required")
        @Size(max = 100, message = "Owner team must not exceed 100 characters")
        String ownerTeam,

        @NotNull(message = "Criticality is required")
        ServiceCriticality criticality,

        String description
) {}
