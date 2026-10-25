package com.incidentiq.ai.dto;

import com.incidentiq.ai.entity.FeedbackRating;
import com.incidentiq.ai.entity.RecommendationStatus;
import jakarta.validation.constraints.Size;

public record RecommendationFeedbackRequest(
        FeedbackRating rating,
        RecommendationStatus actionStatus,
        @Size(max = 2000, message = "Comment must not exceed 2000 characters")
        String comment
) {}
