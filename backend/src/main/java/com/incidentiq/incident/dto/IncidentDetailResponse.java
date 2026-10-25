package com.incidentiq.incident.dto;

import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentSeverity;
import com.incidentiq.incident.entity.IncidentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record IncidentDetailResponse(
        UUID id,
        Long number,
        String formattedNumber,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentPriority priority,
        IncidentStatus status,
        IncidentCategory category,
        UUID serviceId,
        String serviceName,
        UUID reporterId,
        String reporterName,
        UUID assigneeId,
        String assigneeName,
        List<String> tags,
        Instant startedAt,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Instant closedAt,
        Instant escalatedAt,
        int escalationCount,
        String rootCause,
        String resolution,
        Instant slaResponseDueAt,
        Instant slaResolveDueAt,
        boolean slaBreached,
        Set<IncidentStatus> allowedNextStatuses,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static IncidentDetailResponse fromEntity(Incident incident) {
        return new IncidentDetailResponse(
                incident.getId(),
                incident.getNumber(),
                incident.getFormattedNumber(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getSeverity(),
                incident.getPriority(),
                incident.getStatus(),
                incident.getCategory(),
                incident.getService() != null ? incident.getService().getId() : null,
                incident.getService() != null ? incident.getService().getName() : null,
                incident.getReporter() != null ? incident.getReporter().getId() : null,
                incident.getReporter() != null ? incident.getReporter().getFullName() : null,
                incident.getAssignee() != null ? incident.getAssignee().getId() : null,
                incident.getAssignee() != null ? incident.getAssignee().getFullName() : null,
                incident.getTags(),
                incident.getStartedAt(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt(),
                incident.getClosedAt(),
                incident.getEscalatedAt(),
                incident.getEscalationCount(),
                incident.getRootCause(),
                incident.getResolution(),
                incident.getSlaResponseDueAt(),
                incident.getSlaResolveDueAt(),
                incident.isSlaBreached(),
                incident.getStatus().getAllowedNextStatuses(),
                incident.getVersion(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
