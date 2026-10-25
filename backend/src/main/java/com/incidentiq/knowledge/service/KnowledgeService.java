package com.incidentiq.knowledge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.audit.service.AuditService;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.service.CatalogService;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.repository.IncidentRepository;
import com.incidentiq.knowledge.dto.*;
import com.incidentiq.knowledge.entity.*;
import com.incidentiq.knowledge.repository.ChunkSearchProjection;
import com.incidentiq.knowledge.repository.KnowledgeChunkRepository;
import com.incidentiq.knowledge.repository.KnowledgeDocumentRepository;
import com.incidentiq.knowledge.repository.SimilarIncidentProjection;
import com.incidentiq.messaging.service.OutboxService;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Transactional
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final int MAX_DOCUMENT_BYTES = 200 * 1024; // 200 KB

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final MarkdownChunker markdownChunker;
    private final EmbeddingService embeddingService;
    private final CatalogService catalogService;
    private final IncidentRepository incidentRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;

    @Autowired
    public KnowledgeService(KnowledgeDocumentRepository documentRepository,
                            KnowledgeChunkRepository chunkRepository,
                            MarkdownChunker markdownChunker,
                            EmbeddingService embeddingService,
                            CatalogService catalogService,
                            IncidentRepository incidentRepository,
                            AuditService auditService,
                            ObjectMapper objectMapper,
                            @Autowired(required = false) OutboxService outboxService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.markdownChunker = markdownChunker;
        this.embeddingService = embeddingService;
        this.catalogService = catalogService;
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
    }

    public KnowledgeDocumentResponse createAndIngestDocument(CreateDocumentRequest request, User uploader) {
        if (request.content().getBytes(StandardCharsets.UTF_8).length > MAX_DOCUMENT_BYTES) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Document content exceeds 200 KB limit");
        }

        String hash = computeSha256(request.content());
        if (documentRepository.existsByContentHash(hash)) {
            throw new AppException(ErrorCode.CONFLICT, "A document with identical content has already been ingested");
        }

        ServiceEntity service = null;
        if (request.serviceId() != null) {
            service = catalogService.findServiceEntity(request.serviceId());
        }

        AccessLevel accessLevel = request.accessLevel() != null ? request.accessLevel() : AccessLevel.PUBLIC;
        KnowledgeDocument document = new KnowledgeDocument(
                request.title(),
                request.sourceType(),
                hash,
                accessLevel,
                service,
                uploader
        );
        document.setSourceUri(request.sourceUri());
        document.setStatus(DocumentStatus.PROCESSING);
        KnowledgeDocument saved = documentRepository.saveAndFlush(document);

        try {
            List<MarkdownChunker.ChunkResult> chunkResults = markdownChunker.chunkDocument(saved.getTitle(), request.content());
            List<KnowledgeChunk> chunks = new ArrayList<>(chunkResults.size());

            for (MarkdownChunker.ChunkResult cr : chunkResults) {
                float[] embedding = embeddingService.embed(cr.embeddingText());
                KnowledgeChunk chunk = new KnowledgeChunk(
                        saved,
                        cr.chunkIndex(),
                        cr.tokenCount(),
                        cr.content(),
                        embedding,
                        saved.getSourceType(),
                        saved.getAccessLevel(),
                        saved.getService(),
                        cr.metadata()
                );
                chunks.add(chunk);
            }

            chunkRepository.saveAll(chunks);
            saved.setChunkCount(chunks.size());
            saved.setStatus(DocumentStatus.INGESTED);
            documentRepository.saveAndFlush(saved);

            auditService.record(
                    uploader != null ? uploader.getId() : null,
                    "KNOWLEDGE_DOCUMENT_INGESTED",
                    "KnowledgeDocument",
                    saved.getId().toString(),
                    Map.of("title", saved.getTitle(), "chunks", saved.getChunkCount(), "sourceType", saved.getSourceType().name()),
                    "system"
            );

            publishOutboxEvent("KNOWLEDGE", saved.getId(), "knowledge.document.ingested",
                    Map.of("documentId", saved.getId().toString(), "status", saved.getStatus().name(), "chunkCount", saved.getChunkCount()));

            return KnowledgeDocumentResponse.fromEntity(saved);
        } catch (Exception e) {
            log.error("Failed to ingest knowledge document {}: {}", saved.getId(), e.getMessage(), e);
            saved.setStatus(DocumentStatus.FAILED);
            saved.setErrorMessage(e.getMessage());
            documentRepository.saveAndFlush(saved);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Failed to parse and embed document chunks: " + e.getMessage());
        }
    }

    public KnowledgeDocument ingestIncidentAsDocument(Incident incident) {
        String incidentContent = buildIncidentMarkdown(incident);
        String hash = computeSha256(incidentContent);

        Optional<KnowledgeDocument> existing = documentRepository.findByIncidentId(incident.getId());
        KnowledgeDocument document;
        if (existing.isPresent()) {
            document = existing.get();
            if (hash.equals(document.getContentHash())) {
                return document; // already up to date
            }
            document.setContentHash(hash);
            document.setVersion(document.getVersion() + 1);
            document.setStatus(DocumentStatus.PROCESSING);
            chunkRepository.deleteByDocumentId(document.getId());
        } else {
            document = new KnowledgeDocument(
                    "Incident " + incident.getFormattedNumber() + ": " + incident.getTitle(),
                    DocumentSourceType.INCIDENT,
                    hash,
                    AccessLevel.PUBLIC,
                    incident.getService(),
                    incident.getReporter()
            );
            document.setIncident(incident);
            document.setStatus(DocumentStatus.PROCESSING);
        }

        KnowledgeDocument saved = documentRepository.saveAndFlush(document);

        try {
            List<MarkdownChunker.ChunkResult> chunkResults = markdownChunker.chunkDocument(saved.getTitle(), incidentContent);
            List<KnowledgeChunk> chunks = new ArrayList<>(chunkResults.size());

            for (MarkdownChunker.ChunkResult cr : chunkResults) {
                float[] embedding = embeddingService.embed(cr.embeddingText());
                KnowledgeChunk chunk = new KnowledgeChunk(
                        saved,
                        cr.chunkIndex(),
                        cr.tokenCount(),
                        cr.content(),
                        embedding,
                        saved.getSourceType(),
                        saved.getAccessLevel(),
                        saved.getService(),
                        cr.metadata()
                );
                chunks.add(chunk);
            }

            chunkRepository.saveAll(chunks);
            saved.setChunkCount(chunks.size());
            saved.setStatus(DocumentStatus.INGESTED);
            return documentRepository.saveAndFlush(saved);
        } catch (Exception e) {
            log.error("Failed to ingest incident {} as knowledge document: {}", incident.getId(), e.getMessage(), e);
            saved.setStatus(DocumentStatus.FAILED);
            saved.setErrorMessage(e.getMessage());
            return documentRepository.saveAndFlush(saved);
        }
    }

    @Transactional(readOnly = true)
    public List<SearchResultResponse> search(SemanticSearchRequest request, User actor) {
        Set<String> allowedLevels = getAllowedAccessLevelStrings(actor);

        float[] queryVec = embeddingService.embed(request.query());
        String vecStr = embeddingService.toVectorString(queryVec);

        double minSimilarity = request.minSimilarity() != null ? request.minSimilarity() : 0.40;
        int limit = request.limit() != null ? Math.min(Math.max(1, request.limit()), 20) : 5;
        String sourceType = request.sourceType() != null ? request.sourceType().name() : null;

        List<ChunkSearchProjection> projections = chunkRepository.vectorSearch(
                vecStr,
                allowedLevels,
                request.serviceId(),
                sourceType,
                minSimilarity,
                limit
        );

        return projections.stream().map(p -> {
            String snippet = createSnippet(p.getContent());
            String headingPath = parseHeadingPath(p.getMetadata(), p.getDocumentTitle());

            return new SearchResultResponse(
                    p.getChunkId(),
                    p.getDocumentId(),
                    p.getDocumentTitle(),
                    DocumentSourceType.valueOf(p.getSourceType()),
                    AccessLevel.valueOf(p.getAccessLevel()),
                    p.getServiceId(),
                    p.getServiceName(),
                    p.getChunkIndex(),
                    p.getTokenCount(),
                    Math.round(p.getSimilarity() * 100.0) / 100.0,
                    p.getContent(),
                    snippet,
                    headingPath
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<SimilarIncidentResponse> findSimilarIncidents(UUID incidentId, User actor, Double minSimilarity, Integer limit) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Incident not found with id: " + incidentId));

        Set<String> allowedLevels = getAllowedAccessLevelStrings(actor);

        String queryText = incident.getTitle() + "\n" + incident.getDescription() + "\nCategory: " + incident.getCategory();
        float[] queryVec = embeddingService.embed(queryText);
        String vecStr = embeddingService.toVectorString(queryVec);

        double threshold = minSimilarity != null ? minSimilarity : 0.35;
        int topK = limit != null ? Math.min(Math.max(1, limit), 10) : 5;

        List<SimilarIncidentProjection> projections = chunkRepository.findSimilarIncidents(
                vecStr,
                allowedLevels,
                incidentId,
                threshold,
                topK
        );

        return projections.stream().map(p -> new SimilarIncidentResponse(
                p.getDocumentId(),
                p.getIncidentId(),
                p.getIncidentNumber() != null ? String.format("INC-%04d", p.getIncidentNumber()) : null,
                p.getTitle(),
                p.getSeverity(),
                p.getPriority(),
                p.getStatus(),
                p.getRootCause(),
                p.getResolution(),
                p.getResolvedAt(),
                p.getServiceName(),
                Math.round(p.getMaxSimilarity() * 100.0) / 100.0
        )).toList();
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeDocumentResponse> listDocuments(User actor, DocumentSourceType sourceType, UUID serviceId, Pageable pageable) {
        Set<AccessLevel> accessLevels = getAllowedAccessLevels(actor);
        return documentRepository.findWithFilters(accessLevels, sourceType, serviceId, pageable)
                .map(KnowledgeDocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse getDocument(UUID id, User actor) {
        KnowledgeDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Knowledge document not found with id: " + id));

        validateAccess(doc.getAccessLevel(), actor);
        return KnowledgeDocumentResponse.fromEntity(doc);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeChunkResponse> getDocumentChunks(UUID id, User actor) {
        KnowledgeDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Knowledge document not found with id: " + id));

        validateAccess(doc.getAccessLevel(), actor);
        return chunkRepository.findByDocumentIdOrderByChunkIndexAsc(id).stream()
                .map(KnowledgeChunkResponse::fromEntity)
                .toList();
    }

    private void validateAccess(AccessLevel level, User actor) {
        if (level == AccessLevel.ENGINEERING) {
            if (actor == null || (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.ENGINEER)) {
                throw new AppException(ErrorCode.FORBIDDEN, "Access to engineering documents is restricted to engineers and administrators");
            }
        }
    }

    private Set<AccessLevel> getAllowedAccessLevels(User actor) {
        if (actor != null && (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.ENGINEER)) {
            return Set.of(AccessLevel.PUBLIC, AccessLevel.ENGINEERING);
        }
        return Set.of(AccessLevel.PUBLIC);
    }

    private Set<String> getAllowedAccessLevelStrings(User actor) {
        if (actor != null && (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.ENGINEER)) {
            return Set.of("PUBLIC", "ENGINEERING");
        }
        return Set.of("PUBLIC");
    }

    private String buildIncidentMarkdown(Incident inc) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Incident ").append(inc.getFormattedNumber()).append(": ").append(inc.getTitle()).append("\n\n");
        sb.append("## Overview\n");
        sb.append("- **Service**: ").append(inc.getService() != null ? inc.getService().getName() : "Unknown").append("\n");
        sb.append("- **Category**: ").append(inc.getCategory()).append("\n");
        sb.append("- **Severity**: ").append(inc.getSeverity()).append("\n");
        sb.append("- **Priority**: ").append(inc.getPriority()).append("\n");
        sb.append("- **Status**: ").append(inc.getStatus()).append("\n\n");

        sb.append("## Description\n");
        sb.append(inc.getDescription()).append("\n\n");

        if (inc.getRootCause() != null && !inc.getRootCause().isBlank()) {
            sb.append("## Root Cause\n");
            sb.append(inc.getRootCause()).append("\n\n");
        }

        if (inc.getResolution() != null && !inc.getResolution().isBlank()) {
            sb.append("## Resolution\n");
            sb.append(inc.getResolution()).append("\n\n");
        }

        return sb.toString();
    }

    private String createSnippet(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String[] lines = content.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                if (count > 0) sb.append(" ");
                sb.append(trimmed);
                count++;
                if (count >= 3 || sb.length() > 280) break;
            }
        }
        if (content.length() > sb.length()) {
            sb.append("...");
        }
        return sb.toString();
    }

    private String parseHeadingPath(String metadataJson, String fallbackTitle) {
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                Map<String, Object> map = objectMapper.readValue(metadataJson, new TypeReference<>() {});
                Object path = map.get("headingPath");
                if (path != null) {
                    return path.toString();
                }
            } catch (Exception ignored) {
            }
        }
        return fallbackTitle;
    }

    private String computeSha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    private void publishOutboxEvent(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        if (outboxService != null) {
            outboxService.publish(aggregateType, aggregateId, eventType, payload);
        }
    }
}
