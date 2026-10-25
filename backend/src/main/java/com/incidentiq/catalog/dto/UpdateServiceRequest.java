package com.incidentiq.catalog.dto;

import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import jakarta.validation.constraints.Size;

public record UpdateServiceRequest(
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        ServiceEnvironment environment,

        @Size(max = 100, message = "Owner team must not exceed 100 characters")
        String ownerTeam,

        ServiceCriticality criticality,

        String description,

        Boolean active
) {}
