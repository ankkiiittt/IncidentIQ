package com.incidentiq.knowledge.repository;

import java.time.Instant;
import java.util.UUID;

public interface SimilarIncidentProjection {
    UUID getDocumentId();
    UUID getIncidentId();
    Long getIncidentNumber();
    String getTitle();
    String getSeverity();
    String getPriority();
    String getStatus();
    String getRootCause();
    String getResolution();
    Instant getResolvedAt();
    String getServiceName();
    Double getMaxSimilarity();
}
