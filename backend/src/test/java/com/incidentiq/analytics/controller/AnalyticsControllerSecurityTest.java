package com.incidentiq.analytics.controller;

import com.incidentiq.analytics.dto.*;
import com.incidentiq.analytics.service.AnalyticsService;
import com.incidentiq.auth.security.JwtAuthenticationFilter;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.common.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AnalyticsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Unauthenticated request to /api/analytics/overview returns 401")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN role can access all analytics endpoints")
    void testAdminCanAccessAllEndpoints() throws Exception {
        when(analyticsService.getOverview(anyInt()))
                .thenReturn(new AnalyticsOverviewResponse(30, 45.0, 10.0, 95.0, 20L, 18L, 2L, Map.of()));
        when(analyticsService.getVolumeTrend(anyInt(), anyString()))
                .thenReturn(new AnalyticsVolumeResponse(30, "day", List.of()));
        when(analyticsService.getSlaPerformance(anyInt()))
                .thenReturn(new AnalyticsSlaResponse(30, 95.0, List.of()));
        when(analyticsService.getCausesAndRecurrence(anyInt()))
                .thenReturn(new AnalyticsCausesResponse(30, List.of(), List.of()));
        when(analyticsService.getAiFeedbackMetrics(anyInt()))
                .thenReturn(new AiFeedbackMetricsResponse(30, 10L, 8L, 2L, 80.0, 7L, 1L, 87.5, 0.82, 95.0));

        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mttrMinutes").value(45.0));

        mockMvc.perform(get("/api/analytics/volume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("day"));

        mockMvc.perform(get("/api/analytics/sla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallComplianceRate").value(95.0));

        mockMvc.perform(get("/api/analytics/causes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(30));

        mockMvc.perform(get("/api/analytics/ai-feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helpfulRate").value(80.0));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER role can access analytics overview")
    void testViewerCanAccessOverview() throws Exception {
        when(analyticsService.getOverview(anyInt()))
                .thenReturn(new AnalyticsOverviewResponse(30, 30.0, 8.0, 92.0, 10L, 9L, 1L, Map.of()));

        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slaComplianceRate").value(92.0));
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("ENGINEER role can access analytics SLA performance")
    void testEngineerCanAccessSla() throws Exception {
        when(analyticsService.getSlaPerformance(anyInt()))
                .thenReturn(new AnalyticsSlaResponse(30, 88.0, List.of()));

        mockMvc.perform(get("/api/analytics/sla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallComplianceRate").value(88.0));
    }
}
