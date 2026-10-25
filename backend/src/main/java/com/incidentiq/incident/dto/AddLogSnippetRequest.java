package com.incidentiq.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddLogSnippetRequest(
        @NotBlank(message = "Log snippet cannot be blank")
        @Size(max = 4096, message = "Log snippet must not exceed 4 KB (4096 characters)")
        String snippet
) {}
