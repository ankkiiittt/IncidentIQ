package com.incidentiq.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateDeploymentRequest(
        @NotBlank(message = "Version is required")
        @Size(max = 50, message = "Version must not exceed 50 characters")
        String version,

        Instant deployedAt,

        @Size(max = 100, message = "Deployed by must not exceed 100 characters")
        String deployedBy,

        String notes
) {}
