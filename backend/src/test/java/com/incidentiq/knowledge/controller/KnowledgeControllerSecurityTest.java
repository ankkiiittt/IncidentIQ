package com.incidentiq.knowledge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.auth.security.JwtAuthenticationFilter;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.common.config.SecurityConfig;
import com.incidentiq.knowledge.dto.CreateDocumentRequest;
import com.incidentiq.knowledge.dto.SemanticSearchRequest;
import com.incidentiq.knowledge.entity.AccessLevel;
import com.incidentiq.knowledge.entity.DocumentSourceType;
import com.incidentiq.knowledge.service.KnowledgeService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KnowledgeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class KnowledgeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Unauthenticated request to /api/knowledge/documents returns 401")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/knowledge/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER cannot create knowledge document (403 Forbidden)")
    void viewerCannotCreateDocument() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Runbook", DocumentSourceType.RUNBOOK, null, null, AccessLevel.PUBLIC, "Content"
        );

        mockMvc.perform(post("/api/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    @DisplayName("SUPPORT cannot create knowledge document (403 Forbidden)")
    void supportCannotCreateDocument() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                "Runbook", DocumentSourceType.RUNBOOK, null, null, AccessLevel.PUBLIC, "Content"
        );

        mockMvc.perform(post("/api/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("ENGINEER can perform semantic search")
    void engineerCanSearch() throws Exception {
        SemanticSearchRequest request = new SemanticSearchRequest("database latency", null, null, 0.40, 5);
        when(knowledgeService.search(any(), any())).thenReturn(List.of());

        mockMvc.perform(post("/api/knowledge/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
