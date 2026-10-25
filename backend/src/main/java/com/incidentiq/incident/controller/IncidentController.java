package com.incidentiq.incident.controller;

import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.dto.*;
import com.incidentiq.incident.service.IncidentService;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.incidentiq.knowledge.service.KnowledgeService knowledgeService;

    public IncidentController(IncidentService incidentService, UserRepository userRepository) {
        this.incidentService = incidentService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<IncidentSummaryResponse>> searchIncidents(
            IncidentSearchCriteria criteria,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(incidentService.searchIncidents(criteria, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IncidentDetailResponse> getIncident(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncident(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<IncidentDetailResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        User user = getCurrentUser(principal);
        IncidentDetailResponse response = incidentService.createIncident(request, user, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<IncidentDetailResponse> updateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        User user = getCurrentUser(principal);
        IncidentDetailResponse response = incidentService.updateIncident(id, request, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<IncidentDetailResponse> assignIncident(
            @PathVariable UUID id,
            @Valid @RequestBody AssignIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        User user = getCurrentUser(principal);
        IncidentDetailResponse response = incidentService.assignIncident(id, request, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<IncidentDetailResponse> escalateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody EscalateIncidentRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        User user = getCurrentUser(principal);
        IncidentDetailResponse response = incidentService.escalateIncident(id, request, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<IncidentDetailResponse> transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        User user = getCurrentUser(principal);
        IncidentDetailResponse response = incidentService.transitionStatus(id, request, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getCurrentUser(principal);
        CommentResponse response = incidentService.addComment(id, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getComments(id));
    }

    @PostMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT')")
    public ResponseEntity<IncidentEventResponse> addLogSnippet(
            @PathVariable UUID id,
            @Valid @RequestBody AddLogSnippetRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getCurrentUser(principal);
        IncidentEventResponse response = incidentService.addLogSnippet(id, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<IncidentEventResponse>> getTimeline(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getTimeline(id));
    }

    @GetMapping("/{id}/similar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<com.incidentiq.knowledge.dto.SimilarIncidentResponse>> getSimilarIncidents(
            @PathVariable UUID id,
            @RequestParam(required = false) Double minSimilarity,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getCurrentUser(principal);
        if (knowledgeService == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(knowledgeService.findSimilarIncidents(id, user, minSimilarity, limit));
    }

    private User getCurrentUser(UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User authentication required");
        }
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));
    }
}
