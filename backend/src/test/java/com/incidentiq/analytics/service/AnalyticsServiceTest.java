package com.incidentiq.analytics.service;

import com.incidentiq.ai.entity.AnalysisStatus;
import com.incidentiq.ai.entity.FeedbackRating;
import com.incidentiq.ai.entity.RecommendationStatus;
import com.incidentiq.ai.repository.AiAnalysisRepository;
import com.incidentiq.ai.repository.RecommendationRepository;
import com.incidentiq.analytics.dto.*;
import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AiAnalysisRepository aiAnalysisRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(incidentRepository, aiAnalysisRepository, recommendationRepository);
    }

    @Test
    @DisplayName("getOverview calculates MTTR, MTTD, SLA, and priority counts accurately")
    void testGetOverview() {
        when(incidentRepository.calculateResolvedMttrMinutes(any(Instant.class))).thenReturn(42.5);
        when(incidentRepository.calculateMttdMinutes(any(Instant.class))).thenReturn(12.2);
        when(incidentRepository.countSlaMetSince(any(Instant.class))).thenReturn(18L);
        when(incidentRepository.countSlaBreachedSince(any(Instant.class))).thenReturn(2L);
        when(incidentRepository.countByCreatedAtGreaterThanEqual(any(Instant.class))).thenReturn(25L);
        when(incidentRepository.countByResolvedAtGreaterThanEqual(any(Instant.class))).thenReturn(20L);
        when(incidentRepository.countByStatusIn(any())).thenReturn(5L);
        when(incidentRepository.countByPrioritySince(any(Instant.class))).thenReturn(List.of(
                new Object[]{IncidentPriority.P1, 4L},
                new Object[]{IncidentPriority.P2, 8L},
                new Object[]{IncidentPriority.P3, 10L},
                new Object[]{IncidentPriority.P4, 3L}
        ));

        AnalyticsOverviewResponse res = analyticsService.getOverview(30);

        assertThat(res.days()).isEqualTo(30);
        assertThat(res.mttrMinutes()).isEqualTo(42.5);
        assertThat(res.mttdMinutes()).isEqualTo(12.2);
        assertThat(res.slaComplianceRate()).isEqualTo(90.0);
        assertThat(res.totalCreated()).isEqualTo(25L);
        assertThat(res.totalResolved()).isEqualTo(20L);
        assertThat(res.activeCount()).isEqualTo(5L);
        assertThat(res.priorityBreakdown().get(IncidentPriority.P1)).isEqualTo(4L);
        assertThat(res.priorityBreakdown().get(IncidentPriority.P2)).isEqualTo(8L);
    }

    @Test
    @DisplayName("getVolumeTrend returns daily series")
    void testGetVolumeTrendDaily() {
        VolumeTrendProjection p1 = new VolumeTrendProjection() {
            public String getTimestamp() { return "2026-10-24"; }
            public long getP1() { return 1; }
            public long getP2() { return 2; }
            public long getP3() { return 0; }
            public long getP4() { return 0; }
            public long getTotal() { return 3; }
        };
        when(incidentRepository.findVolumeTrendDaily(any(Instant.class))).thenReturn(List.of(p1));

        AnalyticsVolumeResponse res = analyticsService.getVolumeTrend(30, "day");

        assertThat(res.groupBy()).isEqualTo("day");
        assertThat(res.series()).hasSize(1);
        assertThat(res.series().get(0).timestamp()).isEqualTo("2026-10-24");
        assertThat(res.series().get(0).total()).isEqualTo(3);
    }

    @Test
    @DisplayName("getSlaPerformance maps priority targets and rates")
    void testGetSlaPerformance() {
        when(incidentRepository.countSlaMetSince(any(Instant.class))).thenReturn(9L);
        when(incidentRepository.countSlaBreachedSince(any(Instant.class))).thenReturn(1L);

        PrioritySlaProjection p1Proj = new PrioritySlaProjection() {
            public String getPriority() { return "P1"; }
            public long getMetCount() { return 3; }
            public long getBreachedCount() { return 1; }
        };
        when(incidentRepository.findSlaBreakdownByPriority(any(Instant.class))).thenReturn(List.of(p1Proj));

        AnalyticsSlaResponse res = analyticsService.getSlaPerformance(30);

        assertThat(res.overallComplianceRate()).isEqualTo(90.0);
        assertThat(res.byPriority()).hasSize(4);

        PrioritySlaBreakdown p1 = res.byPriority().stream()
                .filter(b -> b.priority() == IncidentPriority.P1)
                .findFirst().orElseThrow();
        assertThat(p1.targetMinutes()).isEqualTo(240);
        assertThat(p1.metCount()).isEqualTo(3);
        assertThat(p1.breachedCount()).isEqualTo(1);
        assertThat(p1.complianceRate()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("getCausesAndRecurrence calculates recurrence and percentages")
    void testGetCausesAndRecurrence() {
        CategoryCountProjection cat1 = new CategoryCountProjection() {
            public String getCategory() { return "DATABASE"; }
            public long getCount() { return 10; }
        };
        when(incidentRepository.findCategoryDistribution(any(Instant.class))).thenReturn(List.of(cat1));

        UUID svcId = UUID.randomUUID();
        RecurringCauseProjection rc1 = new RecurringCauseProjection() {
            public UUID getServiceId() { return svcId; }
            public String getServiceName() { return "checkout-api"; }
            public String getCategory() { return "DATABASE"; }
            public long getIncidentCount() { return 3; }
            public Long getLastIncidentNumber() { return 105L; }
            public Instant getLastOccurredAt() { return Instant.now(); }
        };
        when(incidentRepository.findCausesAndRecurrence(any(Instant.class))).thenReturn(List.of(rc1));

        AnalyticsCausesResponse res = analyticsService.getCausesAndRecurrence(30);

        assertThat(res.categoryDistribution()).hasSize(1);
        assertThat(res.categoryDistribution().get(0).category()).isEqualTo(IncidentCategory.DATABASE);
        assertThat(res.categoryDistribution().get(0).percentage()).isEqualTo(100.0);

        assertThat(res.recurringCauses()).hasSize(1);
        assertThat(res.recurringCauses().get(0).serviceName()).isEqualTo("checkout-api");
        assertThat(res.recurringCauses().get(0).isRecurring()).isTrue();
    }

    @Test
    @DisplayName("getAiFeedbackMetrics calculates helpful and acceptance rates")
    void testGetAiFeedbackMetrics() {
        Instant since = Instant.now().minusSeconds(3600);
        when(aiAnalysisRepository.countByCreatedAtGreaterThanEqualAndStatus(any(Instant.class), eq(AnalysisStatus.COMPLETED))).thenReturn(15L);
        when(recommendationRepository.countByCreatedAtGreaterThanEqualAndFeedbackRating(any(Instant.class), eq(FeedbackRating.HELPFUL))).thenReturn(8L);
        when(recommendationRepository.countByCreatedAtGreaterThanEqualAndFeedbackRating(any(Instant.class), eq(FeedbackRating.NOT_HELPFUL))).thenReturn(2L);
        when(recommendationRepository.countByCreatedAtGreaterThanEqualAndStatus(any(Instant.class), eq(RecommendationStatus.ACCEPTED))).thenReturn(9L);
        when(recommendationRepository.countByCreatedAtGreaterThanEqualAndStatus(any(Instant.class), eq(RecommendationStatus.REJECTED))).thenReturn(1L);
        when(aiAnalysisRepository.getAvgConfidence(any(Instant.class), eq(AnalysisStatus.COMPLETED))).thenReturn(0.85);
        when(aiAnalysisRepository.getAvgLatencyMs(any(Instant.class), eq(AnalysisStatus.COMPLETED))).thenReturn(120.4);

        AiFeedbackMetricsResponse res = analyticsService.getAiFeedbackMetrics(30);

        assertThat(res.totalAnalyses()).isEqualTo(15L);
        assertThat(res.helpfulRate()).isEqualTo(80.0);
        assertThat(res.acceptanceRate()).isEqualTo(90.0);
        assertThat(res.avgConfidence()).isEqualTo(0.85);
        assertThat(res.avgLatencyMs()).isEqualTo(120.4);
    }
}
