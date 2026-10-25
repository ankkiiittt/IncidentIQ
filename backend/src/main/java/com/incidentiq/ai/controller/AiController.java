package com.incidentiq.ai.controller;

import com.incidentiq.ai.dto.AcceptSuggestionRequest;
import com.incidentiq.ai.dto.AiAnalysisResponse;
import com.incidentiq.ai.dto.RecommendationFeedbackRequest;
import com.incidentiq.ai.dto.RecommendationResponse;
import com.incidentiq.ai.entity.AnalysisTrigger;
import com.incidentiq.ai.service.AcceptSuggestionService;
import com.incidentiq.ai.service.AiDiagnosticService;
import com.incidentiq.ai.service.RecommendationFeedbackService;
import com.incidentiq.common.filter.CorrelationIdFilter;
import com.incidentiq.incident.dto.IncidentDetailResponse;
import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AiController {

    private final AiDiagnosticService aiDiagnosticService;
    private final AcceptSuggestionService acceptSuggestionService;
    private final RecommendationFeedbackService recommendationFeedbackService;
    private final UserRepository userRepository;

    public AiController(AiDiagnosticService aiDiagnosticService,
                        AcceptSuggestionService acceptSuggestionService,
                        RecommendationFeedbackService recommendationFeedbackService,
                        UserRepository userRepository) {
        this.aiDiagnosticService = aiDiagnosticService;
        this.acceptSuggestionService = acceptSuggestionService;
        this.recommendationFeedbackService = recommendationFeedbackService;
        this.userRepository = userRepository;
    }

    @PostMapping("/incidents/{id}/ai/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<AiAnalysisResponse> triggerAnalysis(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        User user = resolveUser(principal);
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.CORRELATION_ID_HEADER);
        AiAnalysisResponse response = aiDiagnosticService.analyzeIncident(id, AnalysisTrigger.MANUAL, user, correlationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/incidents/{id}/ai/analyses")
    public ResponseEntity<List<AiAnalysisResponse>> getAnalyses(@PathVariable UUID id) {
        return ResponseEntity.ok(aiDiagnosticService.getAnalysesForIncident(id));
    }

    @GetMapping("/incidents/{id}/ai/analyses/{analysisId}")
    public ResponseEntity<AiAnalysisResponse> getAnalysis(
            @PathVariable UUID id,
            @PathVariable UUID analysisId
    ) {
        return ResponseEntity.ok(aiDiagnosticService.getAnalysis(analysisId));
    }

    @PostMapping("/incidents/{id}/ai/accept-suggestion")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<IncidentDetailResponse> acceptSuggestion(
            @PathVariable UUID id,
            @Valid @RequestBody AcceptSuggestionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(acceptSuggestionService.acceptSuggestion(id, request, user));
    }

    @PostMapping("/recommendations/{id}/feedback")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<RecommendationResponse> recordFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody RecommendationFeedbackRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(recommendationFeedbackService.recordFeedback(id, request, user));
    }

    private User resolveUser(UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            return null;
        }
        return userRepository.findById(principal.getUserId()).orElse(null);
    }
}
