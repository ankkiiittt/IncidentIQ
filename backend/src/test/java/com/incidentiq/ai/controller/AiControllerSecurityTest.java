package com.incidentiq.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.ai.dto.AcceptSuggestionRequest;
import com.incidentiq.ai.dto.RecommendationFeedbackRequest;
import com.incidentiq.ai.entity.FeedbackRating;
import com.incidentiq.ai.entity.RecommendationStatus;
import com.incidentiq.ai.service.AcceptSuggestionService;
import com.incidentiq.ai.service.AiDiagnosticService;
import com.incidentiq.ai.service.RecommendationFeedbackService;
import com.incidentiq.ai.service.SettingsService;
import com.incidentiq.auth.security.JwtAuthenticationFilter;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.common.config.SecurityConfig;
import com.incidentiq.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AiController.class, AdminSettingsController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AiControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiDiagnosticService aiDiagnosticService;

    @MockBean
    private AcceptSuggestionService acceptSuggestionService;

    @MockBean
    private RecommendationFeedbackService recommendationFeedbackService;

    @MockBean
    private SettingsService settingsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Unauthenticated request to trigger AI analysis returns 401")
    void unauthenticatedAnalyzeReturns401() throws Exception {
        mockMvc.perform(post("/api/incidents/" + UUID.randomUUID() + "/ai/analyze"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER cannot trigger AI analysis (403 Forbidden)")
    void viewerCannotTriggerAnalysis() throws Exception {
        mockMvc.perform(post("/api/incidents/" + UUID.randomUUID() + "/ai/analyze"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER can view past AI analyses (200 OK)")
    void viewerCanViewAnalyses() throws Exception {
        UUID incId = UUID.randomUUID();
        when(aiDiagnosticService.getAnalysesForIncident(incId)).thenReturn(List.of());

        mockMvc.perform(get("/api/incidents/" + incId + "/ai/analyses"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER cannot accept suggestions (403 Forbidden)")
    void viewerCannotAcceptSuggestion() throws Exception {
        UUID incId = UUID.randomUUID();
        AcceptSuggestionRequest req = new AcceptSuggestionRequest(true, true, 0L);

        mockMvc.perform(post("/api/incidents/" + incId + "/ai/accept-suggestion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER cannot submit recommendation feedback (403 Forbidden)")
    void viewerCannotSubmitFeedback() throws Exception {
        UUID recId = UUID.randomUUID();
        RecommendationFeedbackRequest req = new RecommendationFeedbackRequest(
                FeedbackRating.HELPFUL, RecommendationStatus.ACCEPTED, "Great runbook"
        );

        mockMvc.perform(post("/api/recommendations/" + recId + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated access to admin settings returns 401")
    void unauthenticatedSettingsReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("ENGINEER cannot access admin settings (403 Forbidden)")
    void engineerCannotAccessAdminSettings() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can access admin settings (200 OK)")
    void adminCanAccessSettings() throws Exception {
        when(settingsService.getAllSettings()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk());
    }
}
