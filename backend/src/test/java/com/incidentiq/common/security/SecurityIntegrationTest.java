package com.incidentiq.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.auth.controller.AuthController;
import com.incidentiq.auth.dto.LoginRequest;
import com.incidentiq.auth.dto.UserProfileResponse;
import com.incidentiq.auth.security.JwtAuthenticationFilter;
import com.incidentiq.auth.service.AuthService;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.auth.service.RefreshTokenService;
import com.incidentiq.common.config.SecurityConfig;
import com.incidentiq.user.controller.UserController;
import com.incidentiq.user.entity.UserRole;
import com.incidentiq.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, AuthController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("Unauthenticated request to /api/users returns 401 with RFC 7807 problem detail")
    void unauthenticatedAccessToUsersReturns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER accessing /api/users returns 403 Forbidden")
    void viewerAccessToUsersReturns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    @DisplayName("SUPPORT accessing /api/users returns 403 Forbidden")
    void supportAccessToUsersReturns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ENGINEER")
    @DisplayName("ENGINEER accessing /api/users returns 403 Forbidden")
    void engineerAccessToUsersReturns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN accessing /api/users succeeds with 200 OK")
    void adminAccessToUsersReturns200() throws Exception {
        when(userService.listUsers(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public login endpoint is accessible without authentication")
    void publicLoginAccessibleWithoutAuth() throws Exception {
        LoginRequest request = new LoginRequest("test@incidentiq.local", "Password123!");
        when(authService.login(any(), any())).thenReturn(
                new AuthService.AuthResult("access-token-123", "refresh-token-456",
                        new UserProfileResponse(java.util.UUID.randomUUID(), "Test", "test@incidentiq.local", UserRole.ADMIN, false))
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"));
    }

    @Test
    @DisplayName("HTTP responses must include defense-in-depth security headers")
    void responsesIncludeHardenedSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Frame-Options", "DENY"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("Content-Security-Policy"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("Permissions-Policy"));
    }
}
