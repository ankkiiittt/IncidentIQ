package com.incidentiq.incident.service;

import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentPriority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class SlaCalculator {

    public Duration getResponseTarget(IncidentPriority priority) {
        if (priority == null) {
            priority = IncidentPriority.P3;
        }
        return switch (priority) {
            case P1 -> Duration.ofMinutes(15);
            case P2 -> Duration.ofMinutes(30);
            case P3 -> Duration.ofMinutes(240); // 4 hours
            case P4 -> Duration.ofMinutes(1440); // 24 hours (1 day)
        };
    }

    public Duration getResolveTarget(IncidentPriority priority) {
        if (priority == null) {
            priority = IncidentPriority.P3;
        }
        return switch (priority) {
            case P1 -> Duration.ofMinutes(240); // 4 hours
            case P2 -> Duration.ofMinutes(480); // 8 hours
            case P3 -> Duration.ofMinutes(4320); // 72 hours (3 days)
            case P4 -> Duration.ofMinutes(10080); // 168 hours (7 days)
        };
    }

    public Instant calculateResponseDue(IncidentPriority priority, Instant baseTime) {
        Instant base = baseTime != null ? baseTime : Instant.now();
        return base.plus(getResponseTarget(priority));
    }

    public Instant calculateResolveDue(IncidentPriority priority, Instant baseTime) {
        Instant base = baseTime != null ? baseTime : Instant.now();
        return base.plus(getResolveTarget(priority));
    }

    public boolean isResponseBreached(Incident incident, Instant now) {
        if (incident.getSlaResponseDueAt() == null) {
            return false;
        }
        Instant effectiveNow = now != null ? now : Instant.now();
        if (incident.getAcknowledgedAt() != null) {
            return incident.getAcknowledgedAt().isAfter(incident.getSlaResponseDueAt());
        }
        return effectiveNow.isAfter(incident.getSlaResponseDueAt());
    }

    public boolean isResolveBreached(Incident incident, Instant now) {
        if (incident.getSlaResolveDueAt() == null) {
            return false;
        }
        Instant effectiveNow = now != null ? now : Instant.now();
        if (incident.getResolvedAt() != null) {
            return incident.getResolvedAt().isAfter(incident.getSlaResolveDueAt());
        }
        return effectiveNow.isAfter(incident.getSlaResolveDueAt());
    }

    public boolean isSlaBreached(Incident incident, Instant now) {
        return isResponseBreached(incident, now) || isResolveBreached(incident, now);
    }
}
