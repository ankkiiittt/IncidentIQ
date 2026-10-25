package com.incidentiq.knowledge.dto;

import com.incidentiq.knowledge.entity.AccessLevel;
import com.incidentiq.knowledge.entity.DocumentSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDocumentRequest(
        @NotBlank(message = "Document title is required")
        @Size(max = 200, message = "Document title must not exceed 200 characters")
        String title,

        @NotNull(message = "Source type is required (RUNBOOK, FAQ, POSTMORTEM)")
        DocumentSourceType sourceType,

        String sourceUri,

        UUID serviceId,

        AccessLevel accessLevel,

        @NotBlank(message = "Content is required")
        String content
) {}
