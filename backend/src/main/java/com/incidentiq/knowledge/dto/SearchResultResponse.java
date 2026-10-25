package com.incidentiq.knowledge.dto;

import com.incidentiq.knowledge.entity.AccessLevel;
import com.incidentiq.knowledge.entity.DocumentSourceType;

import java.util.UUID;

public record SearchResultResponse(
        UUID chunkId,
        UUID documentId,
        String documentTitle,
        DocumentSourceType sourceType,
        AccessLevel accessLevel,
        UUID serviceId,
        String serviceName,
        int chunkIndex,
        int tokenCount,
        double similarity,
        String content,
        String snippet,
        String headingPath
) {}
