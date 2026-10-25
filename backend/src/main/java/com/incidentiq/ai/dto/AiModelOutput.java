package com.incidentiq.ai.dto;

import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;

import java.util.List;

public record AiModelOutput(
        IncidentCategory category,
        IncidentPriority priority,
        String summary,
        boolean evidenceSufficient,
        List<RootCauseHypothesis> rootCauses,
        List<ActionRecommendation> actions
) {
    public record ActionRecommendation(
            String action,
            String rationale
    ) {}
}
