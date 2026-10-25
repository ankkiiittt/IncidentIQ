package com.incidentiq.messaging.dto;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String INCIDENT_CREATED = "incident.created";
    public static final String INCIDENT_UPDATED = "incident.updated";
    public static final String INCIDENT_ESCALATED = "incident.escalated";
    public static final String AI_ANALYSIS_REQUESTED = "ai.analysis.requested";
    public static final String AI_ANALYSIS_COMPLETED = "ai.analysis.completed";
    public static final String KNOWLEDGE_DOCUMENT_INGESTED = "knowledge.document.ingested";
    public static final String SERVICE_METRIC_RECEIVED = "service.metric.received";
    public static final String FAILURE_PREDICTION_CREATED = "failure.prediction.created";

    public static final String DLT_SUFFIX = ".DLT";
}
