package com.incidentiq.ai.service;

import com.incidentiq.ai.dto.RecommendationFeedbackRequest;
import com.incidentiq.ai.dto.RecommendationResponse;
import com.incidentiq.ai.entity.Recommendation;
import com.incidentiq.ai.repository.RecommendationRepository;
import com.incidentiq.audit.service.AuditService;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class RecommendationFeedbackService {

    private final RecommendationRepository recommendationRepository;
    private final AuditService auditService;

    public RecommendationFeedbackService(RecommendationRepository recommendationRepository,
                                         AuditService auditService) {
        this.recommendationRepository = recommendationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RecommendationResponse recordFeedback(UUID recommendationId, RecommendationFeedbackRequest request, User actor) {
        Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Recommendation not found: " + recommendationId));

        if (request.rating() != null) {
            rec.setFeedbackRating(request.rating());
        }
        if (request.actionStatus() != null) {
            rec.setStatus(request.actionStatus());
        }
        if (request.comment() != null) {
            rec.setFeedbackComment(request.comment());
        }

        rec.setFeedbackBy(actor);
        rec.setFeedbackAt(Instant.now());

        Recommendation saved = recommendationRepository.saveAndFlush(rec);

        auditService.record(
                actor != null ? actor.getId() : null,
                "RECOMMENDATION_FEEDBACK_RECORDED",
                "Recommendation",
                recommendationId.toString(),
                Map.of(
                        "rating", String.valueOf(saved.getFeedbackRating()),
                        "status", String.valueOf(saved.getStatus())
                ),
                "system"
        );

        return RecommendationResponse.fromEntity(saved);
    }
}
