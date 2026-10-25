package com.incidentiq.ai.service;

import com.incidentiq.ai.dto.AiModelOutput;

public interface AiClient {

    AiModelOutput completeAnalysis(String systemPrompt, String userPrompt);

    String getModelName();
}
