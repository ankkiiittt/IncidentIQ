package com.incidentiq.incident.dto;

import com.incidentiq.incident.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionStatusRequest(
        @NotNull(message = "Target status is required")
        IncidentStatus status,

        @Size(max = 5000, message = "Root cause must not exceed 5000 characters")
        String rootCause,

        @Size(max = 5000, message = "Resolution must not exceed 5000 characters")
        String resolution,

        @NotNull(message = "Version is required")
        Long version
) {}
