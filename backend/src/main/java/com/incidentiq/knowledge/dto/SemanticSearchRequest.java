package com.incidentiq.knowledge.dto;

import com.incidentiq.knowledge.entity.DocumentSourceType;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SemanticSearchRequest(
        @NotBlank(message = "Search query is required")
        String query,

        UUID serviceId,

        DocumentSourceType sourceType,

        Double minSimilarity,

        Integer limit
) {}
