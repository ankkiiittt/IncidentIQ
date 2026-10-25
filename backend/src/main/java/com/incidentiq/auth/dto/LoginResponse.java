package com.incidentiq.auth.dto;

public record LoginResponse(
        String accessToken,
        UserProfileResponse user
) {}
