package com.incidentiq.incident.entity;

import java.util.Set;

public enum IncidentStatus {
    OPEN,
    ACKNOWLEDGED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public Set<IncidentStatus> getAllowedNextStatuses() {
        return switch (this) {
            case OPEN -> Set.of(ACKNOWLEDGED, IN_PROGRESS);
            case ACKNOWLEDGED -> Set.of(IN_PROGRESS);
            case IN_PROGRESS -> Set.of(RESOLVED);
            case RESOLVED -> Set.of(CLOSED, IN_PROGRESS);
            case CLOSED -> Set.of();
        };
    }

    public boolean canTransitionTo(IncidentStatus target) {
        return getAllowedNextStatuses().contains(target);
    }
}
