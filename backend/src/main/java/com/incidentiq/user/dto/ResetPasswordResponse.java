package com.incidentiq.user.dto;

public record ResetPasswordResponse(
        String temporaryPassword,
        String message
) {}
