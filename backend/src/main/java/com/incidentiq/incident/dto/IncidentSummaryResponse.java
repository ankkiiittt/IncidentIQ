package com.incidentiq.incident.dto;

import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentSeverity;
import com.incidentiq.incident.entity.IncidentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentSummaryResponse(
        UUID id,
        Long number,
        String formattedNumber,
        String title,
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
        boolean slaBreached,
        Instant slaResponseDueAt,
        Instant slaResolveDueAt,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static IncidentSummaryResponse fromEntity(Incident incident) {
        return new IncidentSummaryResponse(
                incident.getId(),
                incident.getNumber(),
                incident.getFormattedNumber(),
                incident.getTitle(),
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
                incident.isSlaBreached(),
                incident.getSlaResponseDueAt(),
                incident.getSlaResolveDueAt(),
                incident.getVersion(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
