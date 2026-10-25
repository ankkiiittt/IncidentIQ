package com.incidentiq.ai.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAppSettingRequest(
        @NotNull(message = "Setting value is required")
        Object value,
        String description
) {}
