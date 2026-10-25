package com.incidentiq.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(
        @NotBlank(message = "Comment body cannot be blank")
        @Size(max = 4000, message = "Comment must not exceed 4000 characters")
        String body
) {}
