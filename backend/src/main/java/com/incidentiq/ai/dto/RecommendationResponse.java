package com.incidentiq.ai.dto;

import com.incidentiq.ai.entity.FeedbackRating;
import com.incidentiq.ai.entity.Recommendation;
import com.incidentiq.ai.entity.RecommendationStatus;

import java.time.Instant;
import java.util.UUID;

public record RecommendationResponse(
        UUID id,
        UUID analysisId,
        int position,
        String action,
        String rationale,
        RecommendationStatus status,
        FeedbackRating feedbackRating,
        String feedbackComment,
        String feedbackByName,
        Instant feedbackAt,
        Instant createdAt
) {
    public static RecommendationResponse fromEntity(Recommendation r) {
        return new RecommendationResponse(
                r.getId(),
                r.getAnalysis().getId(),
                r.getPosition(),
                r.getAction(),
                r.getRationale(),
                r.getStatus(),
                r.getFeedbackRating(),
                r.getFeedbackComment(),
                r.getFeedbackBy() != null ? r.getFeedbackBy().getName() : null,
                r.getFeedbackAt(),
                r.getCreatedAt()
        );
    }
}
