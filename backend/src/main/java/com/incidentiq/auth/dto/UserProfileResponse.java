package com.incidentiq.auth.dto;

import com.incidentiq.user.entity.UserRole;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean mustChangePassword
) {}
