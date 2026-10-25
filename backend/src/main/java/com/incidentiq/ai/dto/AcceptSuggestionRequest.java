package com.incidentiq.ai.dto;

import jakarta.validation.constraints.NotNull;

public record AcceptSuggestionRequest(
        Boolean acceptCategory,
        Boolean acceptPriority,
        @NotNull(message = "Incident version is required for optimistic locking")
        Long version
) {}
