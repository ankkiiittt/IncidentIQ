package com.incidentiq.user.dto;

import com.incidentiq.user.entity.UserRole;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        UserRole role,

        Boolean enabled
) {}
