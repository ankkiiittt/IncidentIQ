package com.incidentiq.metrics.controller;

import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.incident.dto.IncidentDetailResponse;
import com.incidentiq.metrics.dto.CreateIncidentFromPredictionRequest;
import com.incidentiq.metrics.dto.PredictionResponse;
import com.incidentiq.metrics.entity.PredictionRiskLevel;
import com.incidentiq.metrics.service.PredictionService;
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
@RequestMapping("/api/alerts/predictions")
public class PredictionAlertController {

    private final PredictionService predictionService;
    private final UserRepository userRepository;

    public PredictionAlertController(PredictionService predictionService, UserRepository userRepository) {
        this.predictionService = predictionService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PredictionResponse>> listActivePredictions(
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) PredictionRiskLevel riskLevel
    ) {
        return ResponseEntity.ok(predictionService.getActivePredictions(serviceId, riskLevel));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PredictionResponse> getPrediction(@PathVariable UUID id) {
        return ResponseEntity.ok(predictionService.getPrediction(id));
    }

    @PostMapping("/{id}/create-incident")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<IncidentDetailResponse> createIncidentFromPrediction(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CreateIncidentFromPredictionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        User user = resolveUser(principal);
        IncidentDetailResponse response = predictionService.createIncidentFromPrediction(
                id,
                request,
                user,
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<PredictionResponse> dismissPrediction(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        User user = resolveUser(principal);
        PredictionResponse response = predictionService.dismissPrediction(id, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    private User resolveUser(UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            return null;
        }
        return userRepository.findById(principal.getUserId()).orElse(null);
    }
}
