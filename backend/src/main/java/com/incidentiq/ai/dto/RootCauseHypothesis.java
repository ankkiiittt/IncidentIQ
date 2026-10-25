package com.incidentiq.ai.dto;

import java.util.List;

public record RootCauseHypothesis(
        String cause,
        double confidence,
        List<String> evidenceRefs
) {}
