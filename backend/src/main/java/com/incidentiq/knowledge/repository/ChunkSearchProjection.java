package com.incidentiq.knowledge.repository;

import java.util.UUID;

public interface ChunkSearchProjection {
    UUID getChunkId();
    UUID getDocumentId();
    String getDocumentTitle();
    String getSourceType();
    String getAccessLevel();
    UUID getServiceId();
    String getServiceName();
    Integer getChunkIndex();
    Integer getTokenCount();
    String getContent();
    String getMetadata();
    Double getSimilarity();
}
