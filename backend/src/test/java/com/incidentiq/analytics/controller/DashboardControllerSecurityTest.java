package com.incidentiq.analytics.controller;

import com.incidentiq.analytics.dto.*;
import com.incidentiq.analytics.service.DashboardService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    private DashboardResponse stubDashboardResponse() {
        return new DashboardResponse(
                new DashboardKpiResponse(1, 2, 3, 0, 6, 2, 1, 1, 5, 30.5),
                List.of(new DailyTrendPoint("2026-10-25", 2, 1)),
                List.of(),
                List.of(),
                new ServiceHealthSummaryDto(5, 4, 1)
        );
    }

    @Test
    @DisplayName("Unauthenticated access to /api/dashboard returns 401")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN role can access /api/dashboard")
    void adminCanAccessDashboard() throws Exception {
        when(dashboardService.getDashboardData()).thenReturn(stubDashboardResponse());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalOpen").value(6))
                .andExpect(jsonPath("$.kpis.unassigned").value(2));
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("ENGINEER role can access /api/dashboard")
    void engineerCanAccessDashboard() throws Exception {
        when(dashboardService.getDashboardData()).thenReturn(stubDashboardResponse());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalOpen").value(6));
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    @DisplayName("SUPPORT role can access /api/dashboard")
    void supportCanAccessDashboard() throws Exception {
        when(dashboardService.getDashboardData()).thenReturn(stubDashboardResponse());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalOpen").value(6));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER role can access /api/dashboard")
    void viewerCanAccessDashboard() throws Exception {
        when(dashboardService.getDashboardData()).thenReturn(stubDashboardResponse());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalOpen").value(6));
    }
}
