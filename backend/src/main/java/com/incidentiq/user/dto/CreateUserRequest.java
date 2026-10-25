package com.incidentiq.user.dto;

import com.incidentiq.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotNull(message = "Role is required")
        UserRole role,

        String temporaryPassword
) {}
