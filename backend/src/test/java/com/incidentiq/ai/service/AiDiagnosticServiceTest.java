package com.incidentiq.ai.service;

import com.incidentiq.ai.dto.AiAnalysisResponse;
import com.incidentiq.ai.dto.AiModelOutput;
import com.incidentiq.ai.dto.RootCauseHypothesis;
import com.incidentiq.ai.entity.AiAnalysis;
import com.incidentiq.ai.entity.AnalysisStatus;
import com.incidentiq.ai.entity.AnalysisTrigger;
import com.incidentiq.ai.repository.AiAnalysisRepository;
import com.incidentiq.ai.repository.RecommendationRepository;
import com.incidentiq.audit.service.AuditService;
import com.incidentiq.catalog.entity.DeploymentEntity;
import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.repository.DeploymentRepository;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.common.ratelimit.RateLimitResult;
import com.incidentiq.common.ratelimit.RateLimiterService;
import com.incidentiq.incident.entity.*;
import com.incidentiq.incident.repository.IncidentEventRepository;
import com.incidentiq.incident.repository.IncidentRepository;
import com.incidentiq.knowledge.dto.SearchResultResponse;
import com.incidentiq.knowledge.entity.AccessLevel;
import com.incidentiq.knowledge.entity.DocumentSourceType;
import com.incidentiq.knowledge.service.KnowledgeService;
import com.incidentiq.messaging.service.OutboxService;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiDiagnosticServiceTest {

    @Mock
    private AiAnalysisRepository analysisRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentEventRepository incidentEventRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private SettingsService settingsService;

    @Mock
    private AiClient aiClient;

    @Mock
    private AuditService auditService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private OutboxService outboxService;

    private AiDiagnosticService diagnosticService;

    private Incident testIncident;
    private User testUser;
    private ServiceEntity testService;

    @BeforeEach
    void setUp() {
        diagnosticService = new AiDiagnosticService(
                analysisRepository,
                recommendationRepository,
                incidentRepository,
                incidentEventRepository,
                deploymentRepository,
                knowledgeService,
                settingsService,
                aiClient,
                auditService,
                rateLimiterService,
                outboxService
        );

        testUser = new User("Eng User", "engineer@incidentiq.com", "hash", UserRole.ENGINEER);
        testUser.setId(UUID.randomUUID());
        testService = new ServiceEntity("checkout-service", ServiceEnvironment.PROD, "checkout-team", ServiceCriticality.CRITICAL, "Handles checkout");
        testService.setId(UUID.randomUUID());

        testIncident = new Incident(
                "Payment gateway 504 timeout",
                "Transactions failing due to timeout",
                IncidentSeverity.CRITICAL,
                IncidentPriority.P1,
                testService,
                testUser,
                List.of("payment"),
                Instant.now()
        );
        testIncident.setId(UUID.randomUUID());
        testIncident.setCategory(IncidentCategory.APPLICATION);
    }

    private SearchResultResponse createSearchResult(UUID chunkId, UUID docId, String title, DocumentSourceType type, double similarity, String snippet) {
        return new SearchResultResponse(
                chunkId,
                docId,
                title,
                type,
                AccessLevel.PUBLIC,
                testService.getId(),
                testService.getName(),
                0,
                50,
                similarity,
                snippet,
                snippet,
                "Section 1"
        );
    }

    @Test
    @DisplayName("Successfully completes analysis when evidence is found")
    void testAnalyzeIncident_SuccessWithEvidence() {
        UUID incId = testIncident.getId();
        UUID chunkId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        when(settingsService.isAiEnabled()).thenReturn(true);
        when(settingsService.getRagTopK()).thenReturn(5);
        when(settingsService.getRagMinSimilarity()).thenReturn(0.40);
        when(aiClient.getModelName()).thenReturn("deterministic-local-v1");
        when(incidentRepository.findById(incId)).thenReturn(Optional.of(testIncident));

        when(analysisRepository.saveAndFlush(any(AiAnalysis.class))).thenAnswer(inv -> {
            AiAnalysis a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            return a;
        });

        SearchResultResponse searchResult = createSearchResult(
                chunkId,
                docId,
                "Payment Gateway Runbook",
                DocumentSourceType.RUNBOOK,
                0.85,
                "Gateway timeout errors and mitigation steps"
        );
        when(knowledgeService.search(any(), eq(testUser))).thenReturn(List.of(searchResult));

        DeploymentEntity deployment = new DeploymentEntity(testService, "v2.4.1", Instant.now().minusSeconds(1800), "deployer", "notes");
        when(deploymentRepository.findByServiceIdAndDeployedAtAfterOrderByDeployedAtDesc(any(), any()))
                .thenReturn(List.of(deployment));

        AiModelOutput modelOutput = new AiModelOutput(
                IncidentCategory.THIRD_PARTY,
                IncidentPriority.P1,
                "Payment gateway timeout due to downstream provider disruption.",
                true,
                List.of(new RootCauseHypothesis("Downstream network latency", 0.88, List.of(chunkId.toString()))),
                List.of(new AiModelOutput.ActionRecommendation("Failover to secondary payment provider", "Restore transaction flow"))
        );
        when(aiClient.completeAnalysis(anyString(), anyString())).thenReturn(modelOutput);

        AiAnalysisResponse response = diagnosticService.analyzeIncident(incId, AnalysisTrigger.AUTO, testUser, "corr-123");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(response.evidenceSufficient()).isTrue();
        assertThat(response.category()).isEqualTo(IncidentCategory.THIRD_PARTY);
        assertThat(response.suggestedPriority()).isEqualTo(IncidentPriority.P1);
        assertThat(response.rootCauses()).hasSize(1);
        assertThat(response.rootCauses().get(0).evidenceRefs()).contains(chunkId.toString());

        verify(recommendationRepository).saveAll(anyList());
        verify(incidentEventRepository).save(any(IncidentEvent.class));
        verify(auditService).record(eq(testUser.getId()), eq("AI_ANALYSIS_COMPLETED"), eq("AiAnalysis"), any(), any(), eq("system"));
        verify(outboxService).publish(eq("AI"), any(), eq("ai.analysis.completed"), any());
    }

    @Test
    @DisplayName("Sets status to INSUFFICIENT_EVIDENCE when knowledge search yields no chunks")
    void testAnalyzeIncident_InsufficientEvidence() {
        UUID incId = testIncident.getId();

        when(settingsService.isAiEnabled()).thenReturn(true);
        when(settingsService.getRagTopK()).thenReturn(5);
        when(settingsService.getRagMinSimilarity()).thenReturn(0.40);
        when(aiClient.getModelName()).thenReturn("deterministic-local-v1");
        when(incidentRepository.findById(incId)).thenReturn(Optional.of(testIncident));
        when(analysisRepository.saveAndFlush(any(AiAnalysis.class))).thenAnswer(inv -> {
            AiAnalysis a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            return a;
        });

        when(knowledgeService.search(any(), any())).thenReturn(List.of());

        AiAnalysisResponse response = diagnosticService.analyzeIncident(incId, AnalysisTrigger.AUTO, testUser, null);

        assertThat(response.status()).isEqualTo(AnalysisStatus.INSUFFICIENT_EVIDENCE);
        assertThat(response.evidenceSufficient()).isFalse();
        assertThat(response.confidence()).isEqualTo(0.0);
        assertThat(response.summary()).contains("No relevant operational runbooks");

        verify(aiClient, never()).completeAnalysis(anyString(), anyString());
        verify(recommendationRepository, never()).saveAll(anyList());
        verify(incidentEventRepository).save(any(IncidentEvent.class));
        verify(outboxService).publish(eq("AI"), any(), eq("ai.analysis.completed"), any());
    }

    @Test
    @DisplayName("Throws CONFLICT when AI pipeline is disabled in system settings")
    void testAnalyzeIncident_DisabledSetting() {
        when(settingsService.isAiEnabled()).thenReturn(false);

        assertThatThrownBy(() -> diagnosticService.analyzeIncident(testIncident.getId(), AnalysisTrigger.MANUAL, testUser, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("Throws RATE_LIMIT_EXCEEDED when manual trigger exceeds user rate limit")
    void testAnalyzeIncident_RateLimitExceeded() {
        when(settingsService.isAiEnabled()).thenReturn(true);
        when(settingsService.getRateLimitPerHour()).thenReturn(10);
        when(rateLimiterService.checkRateLimit(startsWith("ai:user:"), eq(10), eq(3600)))
                .thenReturn(new RateLimitResult(false, 0, 3600));

        assertThatThrownBy(() -> diagnosticService.analyzeIncident(testIncident.getId(), AnalysisTrigger.MANUAL, testUser, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("Filters out hallucinated evidence chunk IDs not present in RAG search results")
    void testAnalyzeIncident_FiltersHallucinatedCitations() {
        UUID incId = testIncident.getId();
        UUID validChunk = UUID.randomUUID();
        UUID hallucinatedChunk = UUID.randomUUID();

        when(settingsService.isAiEnabled()).thenReturn(true);
        when(settingsService.getRagTopK()).thenReturn(5);
        when(settingsService.getRagMinSimilarity()).thenReturn(0.40);
        when(aiClient.getModelName()).thenReturn("deterministic-local-v1");
        when(incidentRepository.findById(incId)).thenReturn(Optional.of(testIncident));
        when(analysisRepository.saveAndFlush(any(AiAnalysis.class))).thenAnswer(inv -> {
            AiAnalysis a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            return a;
        });

        SearchResultResponse searchResult = createSearchResult(
                validChunk,
                UUID.randomUUID(),
                "Valid Runbook",
                DocumentSourceType.RUNBOOK,
                0.78,
                "Valid snippet"
        );
        when(knowledgeService.search(any(), any())).thenReturn(List.of(searchResult));

        AiModelOutput modelOutput = new AiModelOutput(
                IncidentCategory.DATABASE,
                IncidentPriority.P2,
                "Root cause hypothesis with mixed citations",
                true,
                List.of(new RootCauseHypothesis("Connection pool leak", 0.90, List.of(validChunk.toString(), hallucinatedChunk.toString()))),
                List.of()
        );
        when(aiClient.completeAnalysis(anyString(), anyString())).thenReturn(modelOutput);

        AiAnalysisResponse response = diagnosticService.analyzeIncident(incId, AnalysisTrigger.AUTO, testUser, null);

        assertThat(response.rootCauses()).hasSize(1);
        assertThat(response.rootCauses().get(0).evidenceRefs()).containsExactly(validChunk.toString());
        assertThat(response.rootCauses().get(0).evidenceRefs()).doesNotContain(hallucinatedChunk.toString());
    }
}
