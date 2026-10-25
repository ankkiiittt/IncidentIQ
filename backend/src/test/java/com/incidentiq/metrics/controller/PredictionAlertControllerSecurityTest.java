package com.incidentiq.metrics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.auth.security.JwtAuthenticationFilter;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.common.config.SecurityConfig;
import com.incidentiq.metrics.dto.PredictionResponse;
import com.incidentiq.metrics.entity.PredictionRiskLevel;
import com.incidentiq.metrics.entity.PredictionSource;
import com.incidentiq.metrics.entity.PredictionStatus;
import com.incidentiq.metrics.service.PredictionService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PredictionAlertController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PredictionAlertControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PredictionService predictionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Unauthenticated request to /api/alerts/predictions returns 401")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/alerts/predictions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER can view predictions but cannot dismiss them")
    void testViewerCanViewButNotDismiss() throws Exception {
        UUID id = UUID.randomUUID();
        when(predictionService.getActivePredictions(null, null)).thenReturn(List.of(
                new PredictionResponse(
                        id,
                        UUID.randomUUID(),
                        "checkout-api",
                        BigDecimal.valueOf(0.85),
                        PredictionRiskLevel.HIGH,
                        null,
                        30,
                        PredictionSource.STATISTICAL,
                        "v1",
                        Map.of(),
                        PredictionStatus.ACTIVE,
                        null,
                        Instant.now()
                )
        ));

        mockMvc.perform(get("/api/alerts/predictions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].riskLevel").value("HIGH"));

        mockMvc.perform(post("/api/alerts/predictions/" + id + "/dismiss"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("ENGINEER can dismiss prediction")
    void testEngineerCanDismiss() throws Exception {
        UUID id = UUID.randomUUID();
        when(predictionService.dismissPrediction(any(), any(), any())).thenReturn(
                new PredictionResponse(
                        id,
                        UUID.randomUUID(),
                        "checkout-api",
                        BigDecimal.valueOf(0.85),
                        PredictionRiskLevel.HIGH,
                        null,
                        30,
                        PredictionSource.STATISTICAL,
                        "v1",
                        Map.of(),
                        PredictionStatus.DISMISSED,
                        null,
                        Instant.now()
                )
        );

        mockMvc.perform(post("/api/alerts/predictions/" + id + "/dismiss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }
}
