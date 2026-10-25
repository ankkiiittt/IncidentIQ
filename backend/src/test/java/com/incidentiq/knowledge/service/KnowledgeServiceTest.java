package com.incidentiq.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.audit.service.AuditService;
import com.incidentiq.catalog.service.CatalogService;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.repository.IncidentRepository;
import com.incidentiq.knowledge.dto.CreateDocumentRequest;
import com.incidentiq.knowledge.dto.KnowledgeDocumentResponse;
import com.incidentiq.knowledge.dto.SearchResultResponse;
import com.incidentiq.knowledge.dto.SemanticSearchRequest;
import com.incidentiq.knowledge.entity.AccessLevel;
import com.incidentiq.knowledge.entity.DocumentSourceType;
import com.incidentiq.knowledge.entity.KnowledgeDocument;
import com.incidentiq.knowledge.repository.KnowledgeChunkRepository;
import com.incidentiq.knowledge.repository.KnowledgeDocumentRepository;
import com.incidentiq.messaging.service.OutboxService;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    @Mock
    private CatalogService catalogService;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private OutboxService outboxService;

    private final MarkdownChunker markdownChunker = new MarkdownChunker();
    private final LocalDeterministicEmbeddingService embeddingService = new LocalDeterministicEmbeddingService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(
                documentRepository,
                chunkRepository,
                markdownChunker,
                embeddingService,
                catalogService,
                incidentRepository,
                auditService,
                objectMapper,
                outboxService
        );
    }

    @Test
    @DisplayName("Successfully ingests document with chunks and embeddings")
    void testCreateAndIngestDocumentSuccess() {
        User admin = new User("Admin", "admin@incidentiq.local", "hash", UserRole.ADMIN);
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Redis Failover Runbook",
                DocumentSourceType.RUNBOOK,
                "https://wiki.test/redis",
                null,
                AccessLevel.PUBLIC,
                "# Redis Failover\n\n## Step 1\nRun redis-cli failover."
        );

        when(documentRepository.existsByContentHash(anyString())).thenReturn(false);
        when(documentRepository.saveAndFlush(any(KnowledgeDocument.class))).thenAnswer(inv -> {
            KnowledgeDocument doc = inv.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID());
            }
            return doc;
        });

        KnowledgeDocumentResponse response = knowledgeService.createAndIngestDocument(request, admin);

        assertNotNull(response);
        assertEquals("Redis Failover Runbook", response.title());
        assertEquals(DocumentSourceType.RUNBOOK, response.sourceType());
        assertTrue(response.chunkCount() > 0);

        verify(chunkRepository).saveAll(anyList());
        verify(auditService).record(any(), eq("KNOWLEDGE_DOCUMENT_INGESTED"), eq("KnowledgeDocument"), any(), any(), eq("system"));
    }

    @Test
    @DisplayName("Rejects document with duplicate content hash")
    void testDuplicateDocumentRejected() {
        User admin = new User("Admin", "admin@incidentiq.local", "hash", UserRole.ADMIN);
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Duplicate Title",
                DocumentSourceType.RUNBOOK,
                null,
                null,
                AccessLevel.PUBLIC,
                "Duplicate content text"
        );

        when(documentRepository.existsByContentHash(anyString())).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () ->
                knowledgeService.createAndIngestDocument(request, admin));

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        verify(documentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Rejects document exceeding 200 KB maximum size")
    void testOversizedDocumentRejected() {
        User admin = new User("Admin", "admin@incidentiq.local", "hash", UserRole.ADMIN);
        String hugeContent = "A".repeat(201 * 1024);
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Huge Title",
                DocumentSourceType.RUNBOOK,
                null,
                null,
                AccessLevel.PUBLIC,
                hugeContent
        );

        AppException ex = assertThrows(AppException.class, () ->
                knowledgeService.createAndIngestDocument(request, admin));

        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Vector search enforces pre-retrieval role access levels (SUPPORT role gets PUBLIC only)")
    void testSearchAccessLevelSupportRole() {
        User supportUser = new User("Support", "sup@test.com", "hash", UserRole.SUPPORT);
        SemanticSearchRequest request = new SemanticSearchRequest(
                "database latency",
                null,
                null,
                0.40,
                5
        );

        when(chunkRepository.vectorSearch(anyString(), anyCollection(), any(), any(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        List<SearchResultResponse> results = knowledgeService.search(request, supportUser);
        assertNotNull(results);

        // Verify chunkRepository was queried with ONLY 'PUBLIC' access level
        verify(chunkRepository).vectorSearch(
                anyString(),
                argThat(levels -> levels.size() == 1 && levels.contains("PUBLIC")),
                isNull(),
                isNull(),
                eq(0.40),
                eq(5)
        );
    }

    @Test
    @DisplayName("Vector search allows both PUBLIC and ENGINEERING access levels for ENGINEER")
    void testSearchAccessLevelEngineerRole() {
        User engineer = new User("Engineer", "eng@test.com", "hash", UserRole.ENGINEER);
        SemanticSearchRequest request = new SemanticSearchRequest(
                "database latency",
                null,
                null,
                0.40,
                5
        );

        when(chunkRepository.vectorSearch(anyString(), anyCollection(), any(), any(), anyDouble(), anyInt()))
                .thenReturn(List.of());

        knowledgeService.search(request, engineer);

        // Verify chunkRepository was queried with both 'PUBLIC' and 'ENGINEERING'
        verify(chunkRepository).vectorSearch(
                anyString(),
                argThat(levels -> levels.size() == 2 && levels.contains("PUBLIC") && levels.contains("ENGINEERING")),
                isNull(),
                isNull(),
                eq(0.40),
                eq(5)
        );
    }
}
