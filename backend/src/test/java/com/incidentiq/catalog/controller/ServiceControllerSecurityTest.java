package com.incidentiq.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.auth.security.JwtAuthenticationFilter;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.catalog.dto.CreateDeploymentRequest;
import com.incidentiq.catalog.dto.CreateServiceRequest;
import com.incidentiq.catalog.dto.DeploymentResponse;
import com.incidentiq.catalog.dto.ServiceResponse;
import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import com.incidentiq.catalog.service.CatalogService;
import com.incidentiq.common.config.SecurityConfig;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ServiceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ServiceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogService catalogService;

    @MockBean
    private com.incidentiq.metrics.service.ServiceHealthService serviceHealthService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Unauthenticated access to /api/services returns 401")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER can list services")
    void viewerCanListServices() throws Exception {
        when(catalogService.listServices(eq(false), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("ENGINEER cannot create service (ADMIN only)")
    void engineerCannotCreateService() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest(
                "auth-service", ServiceEnvironment.PROD, "SecOps", ServiceCriticality.CRITICAL, "Desc"
        );

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can create service")
    void adminCanCreateService() throws Exception {
        CreateServiceRequest request = new CreateServiceRequest(
                "auth-service", ServiceEnvironment.PROD, "SecOps", ServiceCriticality.CRITICAL, "Desc"
        );
        ServiceResponse response = new ServiceResponse(
                UUID.randomUUID(), "auth-service", ServiceEnvironment.PROD, "SecOps",
                ServiceCriticality.CRITICAL, "Desc", true, Instant.now(), Instant.now()
        );
        when(catalogService.createService(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("auth-service"));
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    @DisplayName("SUPPORT cannot deactivate service")
    void supportCannotDeactivateService() throws Exception {
        mockMvc.perform(delete("/api/services/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can deactivate service")
    void adminCanDeactivateService() throws Exception {
        UUID serviceId = UUID.randomUUID();
        doNothing().when(catalogService).deactivateService(eq(serviceId), any(), any());

        mockMvc.perform(delete("/api/services/" + serviceId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("ENGINEER can record deployment")
    void engineerCanRecordDeployment() throws Exception {
        UUID serviceId = UUID.randomUUID();
        CreateDeploymentRequest request = new CreateDeploymentRequest("v2.1.0", Instant.now(), "dev-lead", "Release");
        DeploymentResponse response = new DeploymentResponse(
                UUID.randomUUID(), serviceId, "auth-service", "v2.1.0", Instant.now(), "dev-lead", "Release", Instant.now()
        );
        when(catalogService.recordDeployment(eq(serviceId), any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/services/" + serviceId + "/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value("v2.1.0"));
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    @DisplayName("SUPPORT cannot record deployment")
    void supportCannotRecordDeployment() throws Exception {
        UUID serviceId = UUID.randomUUID();
        CreateDeploymentRequest request = new CreateDeploymentRequest("v2.1.0", Instant.now(), "dev-lead", "Release");

        mockMvc.perform(post("/api/services/" + serviceId + "/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
