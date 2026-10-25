package com.incidentiq.knowledge.controller;

import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.knowledge.dto.*;
import com.incidentiq.knowledge.entity.DocumentSourceType;
import com.incidentiq.knowledge.service.KnowledgeService;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.repository.UserRepository;
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
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final UserRepository userRepository;

    public KnowledgeController(KnowledgeService knowledgeService, UserRepository userRepository) {
        this.knowledgeService = knowledgeService;
        this.userRepository = userRepository;
    }

    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<KnowledgeDocumentResponse> createDocument(
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = resolveUser(principal);
        KnowledgeDocumentResponse response = knowledgeService.createAndIngestDocument(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<KnowledgeDocumentResponse>> listDocuments(
            @RequestParam(required = false) DocumentSourceType sourceType,
            @RequestParam(required = false) UUID serviceId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(knowledgeService.listDocuments(user, sourceType, serviceId, pageable));
    }

    @GetMapping("/documents/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KnowledgeDocumentResponse> getDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(knowledgeService.getDocument(id, user));
    }

    @GetMapping("/documents/{id}/chunks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KnowledgeChunkResponse>> getDocumentChunks(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(knowledgeService.getDocumentChunks(id, user));
    }

    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SearchResultResponse>> search(
            @Valid @RequestBody SemanticSearchRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(knowledgeService.search(request, user));
    }

    private User resolveUser(UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            return null;
        }
        return userRepository.findById(principal.getUserId()).orElse(null);
    }
}
