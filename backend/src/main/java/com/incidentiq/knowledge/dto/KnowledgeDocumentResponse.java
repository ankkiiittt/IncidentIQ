package com.incidentiq.knowledge.dto;

import com.incidentiq.knowledge.entity.AccessLevel;
import com.incidentiq.knowledge.entity.DocumentSourceType;
import com.incidentiq.knowledge.entity.DocumentStatus;
import com.incidentiq.knowledge.entity.KnowledgeDocument;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocumentResponse(
        UUID id,
        String title,
        DocumentSourceType sourceType,
        String sourceUri,
        UUID incidentId,
        UUID serviceId,
        String serviceName,
        AccessLevel accessLevel,
        String contentHash,
        DocumentStatus status,
        int chunkCount,
        String errorMessage,
        String uploadedByName,
        Instant createdAt,
        Instant updatedAt
) {
    public static KnowledgeDocumentResponse fromEntity(KnowledgeDocument doc) {
        return new KnowledgeDocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getSourceType(),
                doc.getSourceUri(),
                doc.getIncident() != null ? doc.getIncident().getId() : null,
                doc.getService() != null ? doc.getService().getId() : null,
                doc.getService() != null ? doc.getService().getName() : null,
                doc.getAccessLevel(),
                doc.getContentHash(),
                doc.getStatus(),
                doc.getChunkCount(),
                doc.getErrorMessage(),
                doc.getUploadedBy() != null ? doc.getUploadedBy().getFullName() : null,
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
