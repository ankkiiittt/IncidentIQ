package com.incidentiq.incident.dto;

import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentSeverity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateIncidentRequest(
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description,

        IncidentSeverity severity,

        IncidentCategory category,

        List<String> tags,

        @NotNull(message = "Version is required for optimistic concurrency check")
        Long version
) {}
