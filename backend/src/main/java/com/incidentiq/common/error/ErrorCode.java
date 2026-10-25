package com.incidentiq.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Input validation failed"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", HttpStatus.UNAUTHORIZED, "Account is temporarily locked due to multiple failed login attempts"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Authentication token has expired"),
    TOKEN_REVOKED("TOKEN_REVOKED", HttpStatus.UNAUTHORIZED, "Authentication token has been revoked"),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "Access is denied"),
    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "Resource conflict"),
    STALE_VERSION("STALE_VERSION", HttpStatus.CONFLICT, "The resource was modified by another transaction. Please reload."),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please retry later."),
    AI_SERVICE_UNAVAILABLE("AI_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "AI analysis provider is currently unavailable"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
