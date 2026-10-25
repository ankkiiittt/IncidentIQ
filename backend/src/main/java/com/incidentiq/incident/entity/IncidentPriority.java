package com.incidentiq.incident.entity;

public enum IncidentPriority {
    P1,
    P2,
    P3,
    P4;

    public IncidentPriority escalate() {
        return switch (this) {
            case P4 -> P3;
            case P3 -> P2;
            case P2, P1 -> P1;
        };
    }
}
