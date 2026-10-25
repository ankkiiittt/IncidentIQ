package com.incidentiq.ai.service;

import com.incidentiq.ai.dto.AcceptSuggestionRequest;
import com.incidentiq.ai.entity.AiAnalysis;
import com.incidentiq.ai.entity.AnalysisStatus;
import com.incidentiq.ai.repository.AiAnalysisRepository;
import com.incidentiq.audit.service.AuditService;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.dto.IncidentDetailResponse;
import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentEvent;
import com.incidentiq.incident.entity.IncidentEventType;
import com.incidentiq.incident.repository.IncidentEventRepository;
import com.incidentiq.incident.repository.IncidentRepository;
import com.incidentiq.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AcceptSuggestionService {

    private final AiAnalysisRepository analysisRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final AuditService auditService;

    public AcceptSuggestionService(AiAnalysisRepository analysisRepository,
                                   IncidentRepository incidentRepository,
                                   IncidentEventRepository incidentEventRepository,
                                   AuditService auditService) {
        this.analysisRepository = analysisRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.auditService = auditService;
    }

    @Transactional
    public IncidentDetailResponse acceptSuggestion(UUID incidentId, AcceptSuggestionRequest request, User actor) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Incident not found: " + incidentId));

        if (!incident.getVersion().equals(request.version())) {
            throw new AppException(ErrorCode.STALE_VERSION, "The incident was modified by another transaction. Please reload.");
        }

        AiAnalysis latestAnalysis = analysisRepository.findFirstByIncidentIdOrderByCreatedAtDesc(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "No AI analysis found for incident: " + incidentId));

        if (latestAnalysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new AppException(ErrorCode.CONFLICT, "Cannot accept suggestions from non-completed analysis: " + latestAnalysis.getStatus());
        }

        boolean categoryChanged = false;
        if (Boolean.TRUE.equals(request.acceptCategory()) && latestAnalysis.getCategory() != null) {
            incident.setCategory(latestAnalysis.getCategory());
            categoryChanged = true;
        }

        boolean priorityChanged = false;
        if (Boolean.TRUE.equals(request.acceptPriority()) && latestAnalysis.getSuggestedPriority() != null) {
            incident.setPriority(latestAnalysis.getSuggestedPriority());
            priorityChanged = true;
        }

        Incident saved = incidentRepository.saveAndFlush(incident);

        if (priorityChanged) {
            incidentEventRepository.save(new IncidentEvent(
                    saved,
                    IncidentEventType.PRIORITY_CHANGED,
                    actor,
                    Map.of("priority", saved.getPriority().name(), "reason", "Accepted AI triage recommendation")
            ));
        }

        auditService.record(
                actor != null ? actor.getId() : null,
                "AI_SUGGESTION_ACCEPTED",
                "Incident",
                incidentId.toString(),
                Map.of(
                        "acceptedCategory", categoryChanged,
                        "category", String.valueOf(saved.getCategory()),
                        "acceptedPriority", priorityChanged,
                        "priority", String.valueOf(saved.getPriority())
                ),
                "system"
        );

        return IncidentDetailResponse.fromEntity(saved);
    }
}
