package com.incidentiq.incident.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EscalateIncidentRequest(
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        @NotNull(message = "Version is required")
        Long version
) {}
