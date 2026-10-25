package com.incidentiq.incident.entity;

public enum IncidentEventType {
    CREATED,
    STATUS_CHANGED,
    ASSIGNED,
    ESCALATED,
    PRIORITY_CHANGED,
    COMMENT_ADDED,
    LOG_ADDED,
    DEPLOYMENT_LINKED,
    AI_ANALYSIS_COMPLETED,
    AI_ANALYSIS_FAILED,
    PREDICTION_LINKED
}
