package com.incidentiq.incident.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.auth.security.JwtAuthenticationFilter;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.common.config.SecurityConfig;
import com.incidentiq.incident.dto.*;
import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentSeverity;
import com.incidentiq.incident.service.IncidentService;
import com.incidentiq.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IncidentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class IncidentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncidentService incidentService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Unauthenticated access to /api/incidents returns 401")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER can list incidents")
    void viewerCanListIncidents() throws Exception {
        when(incidentService.searchIncidents(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER cannot create incident")
    void viewerCannotCreateIncident() throws Exception {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "DB connection failure", "Cannot connect to master DB",
                IncidentSeverity.HIGH, IncidentPriority.P2, IncidentCategory.DATABASE,
                UUID.randomUUID(), List.of("db"), null, null
        );

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    @DisplayName("SUPPORT cannot assign incident (ADMIN/ENGINEER only)")
    void supportCannotAssignIncident() throws Exception {
        UUID incidentId = UUID.randomUUID();
        AssignIncidentRequest request = new AssignIncidentRequest(UUID.randomUUID(), 0L);

        mockMvc.perform(post("/api/incidents/" + incidentId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER cannot add comment")
    void viewerCannotAddComment() throws Exception {
        UUID incidentId = UUID.randomUUID();
        AddCommentRequest request = new AddCommentRequest("Investigating now");

        mockMvc.perform(post("/api/incidents/" + incidentId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER cannot add log snippet")
    void viewerCannotAddLogSnippet() throws Exception {
        UUID incidentId = UUID.randomUUID();
        AddLogSnippetRequest request = new AddLogSnippetRequest("Exception in thread main");

        mockMvc.perform(post("/api/incidents/" + incidentId + "/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
