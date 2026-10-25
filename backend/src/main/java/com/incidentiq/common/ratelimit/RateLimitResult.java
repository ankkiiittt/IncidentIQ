package com.incidentiq.common.ratelimit;

public record RateLimitResult(
        boolean allowed,
        long remaining,
        long resetSeconds
) {}
