package com.incidentiq.analytics.dto;

public record ServiceHealthSummaryDto(
        long totalServices,
        long healthyServices,
        long degradedServices
) {}
