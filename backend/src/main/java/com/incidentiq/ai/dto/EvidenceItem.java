package com.incidentiq.ai.dto;

import java.util.UUID;

public record EvidenceItem(
        UUID chunkId,
        UUID documentId,
        String title,
        String sourceType,
        double similarity,
        String snippet
) {}
