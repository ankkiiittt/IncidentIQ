package com.incidentiq.knowledge.dto;

import com.incidentiq.knowledge.entity.AccessLevel;
import com.incidentiq.knowledge.entity.DocumentSourceType;
import com.incidentiq.knowledge.entity.KnowledgeChunk;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record KnowledgeChunkResponse(
        UUID id,
        UUID documentId,
        int chunkIndex,
        int tokenCount,
        String content,
        DocumentSourceType sourceType,
        AccessLevel accessLevel,
        UUID serviceId,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public static KnowledgeChunkResponse fromEntity(KnowledgeChunk chunk) {
        return new KnowledgeChunkResponse(
                chunk.getId(),
                chunk.getDocument().getId(),
                chunk.getChunkIndex(),
                chunk.getTokenCount(),
                chunk.getContent(),
                chunk.getSourceType(),
                chunk.getAccessLevel(),
                chunk.getService() != null ? chunk.getService().getId() : null,
                chunk.getMetadata(),
                chunk.getCreatedAt()
        );
    }
}
