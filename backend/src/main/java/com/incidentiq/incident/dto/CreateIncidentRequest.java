package com.incidentiq.incident.dto;

import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateIncidentRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description,

        @NotNull(message = "Severity is required")
        IncidentSeverity severity,

        IncidentPriority priority,

        IncidentCategory category,

        @NotNull(message = "Service ID is required")
        UUID serviceId,

        List<String> tags,

        Instant startedAt,

        @Size(max = 4096, message = "Log snippet must not exceed 4 KB")
        String logSnippet
) {}
