package com.incidentiq.incident.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignIncidentRequest(
        @NotNull(message = "Assignee ID is required")
        UUID assigneeId,

        @NotNull(message = "Version is required")
        Long version
) {}
