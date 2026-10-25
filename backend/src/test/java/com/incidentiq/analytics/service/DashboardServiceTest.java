package com.incidentiq.analytics.service;

import com.incidentiq.analytics.dto.*;
import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import com.incidentiq.catalog.repository.ServiceRepository;
import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentSeverity;
import com.incidentiq.incident.entity.IncidentStatus;
import com.incidentiq.incident.repository.IncidentRepository;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ServiceRepository serviceRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(incidentRepository, serviceRepository);
    }

    @Test
    @DisplayName("getDashboardData aggregates all KPIs, trends, top services, and health status")
    void testGetDashboardData() {
        Set<IncidentStatus> activeStatuses = Set.of(
                IncidentStatus.OPEN,
                IncidentStatus.ACKNOWLEDGED,
                IncidentStatus.IN_PROGRESS
        );

        when(incidentRepository.countByPriorityAndStatusIn(IncidentPriority.P1, activeStatuses)).thenReturn(2L);
        when(incidentRepository.countByPriorityAndStatusIn(IncidentPriority.P2, activeStatuses)).thenReturn(3L);
        when(incidentRepository.countByPriorityAndStatusIn(IncidentPriority.P3, activeStatuses)).thenReturn(5L);
        when(incidentRepository.countByPriorityAndStatusIn(IncidentPriority.P4, activeStatuses)).thenReturn(1L);
        when(incidentRepository.countByStatusIn(activeStatuses)).thenReturn(11L);
        when(incidentRepository.countByAssigneeIsNullAndStatusIn(activeStatuses)).thenReturn(4L);
        when(incidentRepository.countBySlaBreachedTrueAndStatusIn(activeStatuses)).thenReturn(2L);
        when(incidentRepository.countSlaAtRisk(eq(activeStatuses), any(Instant.class))).thenReturn(3L);
        when(incidentRepository.countByResolvedAtGreaterThanEqual(any(Instant.class))).thenReturn(7L);
        when(incidentRepository.calculateMttrMinutes(any(Instant.class))).thenReturn(45.678);

        // Daily trend projection mock
        DailyIncidentTrendProjection trendMock = new DailyIncidentTrendProjection() {
            @Override
            public String getDay() { return "2026-10-25"; }
            @Override
            public long getCreatedCount() { return 5L; }
            @Override
            public long getResolvedCount() { return 3L; }
        };
        when(incidentRepository.find30DayTrend()).thenReturn(List.of(trendMock));

        // Top affected services projection mock
        UUID serviceId = UUID.randomUUID();
        TopAffectedServiceProjection serviceMock = new TopAffectedServiceProjection() {
            @Override
            public UUID getServiceId() { return serviceId; }
            @Override
            public String getServiceName() { return "Order Service"; }
            @Override
            public String getServiceTier() { return "TIER_1"; }
            @Override
            public long getActiveIncidents() { return 4L; }
            @Override
            public long getTotalIncidents() { return 10L; }
        };
        when(incidentRepository.findTopAffectedServices()).thenReturn(List.of(serviceMock));

        // Recent incidents mock
        User reporter = new User("Jane Doe", "jane@example.com", "hash", UserRole.ENGINEER);
        ServiceEntity service = new ServiceEntity("Order Service", ServiceEnvironment.PROD, "Order Team",
                ServiceCriticality.CRITICAL, "Order desc");
        Incident incident = new Incident("Checkout failure", "desc", IncidentSeverity.HIGH,
                IncidentPriority.P2, service, reporter, List.of("checkout"), Instant.now());
        ReflectionTestUtils.setField(incident, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(incident, "number", 101L);
        when(incidentRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(incident));

        // Service health summary mock
        when(serviceRepository.countByActiveTrue()).thenReturn(8L);
        when(incidentRepository.countDistinctServiceByStatusIn(activeStatuses)).thenReturn(2L);

        // Execute
        DashboardResponse response = dashboardService.getDashboardData();

        // Verify KPIs
        assertThat(response.kpis().openP1()).isEqualTo(2L);
        assertThat(response.kpis().openP2()).isEqualTo(3L);
        assertThat(response.kpis().openP3()).isEqualTo(5L);
        assertThat(response.kpis().openP4()).isEqualTo(1L);
        assertThat(response.kpis().totalOpen()).isEqualTo(11L);
        assertThat(response.kpis().unassigned()).isEqualTo(4L);
        assertThat(response.kpis().slaBreached()).isEqualTo(2L);
        assertThat(response.kpis().slaAtRisk()).isEqualTo(3L);
        assertThat(response.kpis().resolvedLast7Days()).isEqualTo(7L);
        assertThat(response.kpis().mttrMinutes()).isEqualTo(45.7);

        // Verify Trend
        assertThat(response.trend30Days()).hasSize(1);
        assertThat(response.trend30Days().get(0).date()).isEqualTo("2026-10-25");
        assertThat(response.trend30Days().get(0).createdCount()).isEqualTo(5L);
        assertThat(response.trend30Days().get(0).resolvedCount()).isEqualTo(3L);

        // Verify Top Affected Services
        assertThat(response.topAffectedServices()).hasSize(1);
        assertThat(response.topAffectedServices().get(0).serviceName()).isEqualTo("Order Service");
        assertThat(response.topAffectedServices().get(0).activeIncidents()).isEqualTo(4L);

        // Verify Recent Incidents
        assertThat(response.recentIncidents()).hasSize(1);
        assertThat(response.recentIncidents().get(0).title()).isEqualTo("Checkout failure");

        // Verify Service Health
        assertThat(response.serviceHealth().totalServices()).isEqualTo(8L);
        assertThat(response.serviceHealth().healthyServices()).isEqualTo(6L);
        assertThat(response.serviceHealth().degradedServices()).isEqualTo(2L);
    }
}
