package com.incidentiq.ai.dto;

import com.incidentiq.ai.entity.AiAnalysis;
import com.incidentiq.ai.entity.AnalysisStatus;
import com.incidentiq.ai.entity.AnalysisTrigger;
import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiAnalysisResponse(
        UUID id,
        UUID incidentId,
        AnalysisTrigger trigger,
        String requestedByName,
        AnalysisStatus status,
        String model,
        String embeddingModel,
        String promptVersion,
        IncidentCategory category,
        IncidentPriority suggestedPriority,
        String summary,
        Double confidence,
        List<RootCauseHypothesis> rootCauses,
        List<EvidenceItem> evidence,
        boolean evidenceSufficient,
        Integer inputTokens,
        Integer outputTokens,
        Integer latencyMs,
        String errorCode,
        Instant createdAt,
        Instant completedAt,
        List<RecommendationResponse> recommendations
) {
    public static AiAnalysisResponse fromEntity(AiAnalysis a) {
        List<RecommendationResponse> recs = a.getRecommendations() != null
                ? a.getRecommendations().stream().map(RecommendationResponse::fromEntity).toList()
                : List.of();

        Double conf = a.getConfidence() != null ? a.getConfidence().doubleValue() : null;

        return new AiAnalysisResponse(
                a.getId(),
                a.getIncident().getId(),
                a.getTrigger(),
                a.getRequestedBy() != null ? a.getRequestedBy().getName() : null,
                a.getStatus(),
                a.getModel(),
                a.getEmbeddingModel(),
                a.getPromptVersion(),
                a.getCategory(),
                a.getSuggestedPriority(),
                a.getSummary(),
                conf,
                a.getRootCauses() != null ? a.getRootCauses() : List.of(),
                a.getEvidence() != null ? a.getEvidence() : List.of(),
                a.isEvidenceSufficient(),
                a.getInputTokens(),
                a.getOutputTokens(),
                a.getLatencyMs(),
                a.getErrorCode(),
                a.getCreatedAt(),
                a.getCompletedAt(),
                recs
        );
    }
}
