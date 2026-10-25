package com.incidentiq.notification.controller;

import com.incidentiq.auth.security.JwtAuthenticationFilter;
import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.common.config.SecurityConfig;
import com.incidentiq.notification.dto.NotificationResponse;
import com.incidentiq.notification.entity.NotificationType;
import com.incidentiq.notification.service.NotificationService;
import com.incidentiq.user.entity.UserRole;
import com.incidentiq.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    private UserPrincipal createMockPrincipal(UUID userId) {
        return new UserPrincipal(
                userId,
                "engineer@incidentiq.com",
                "Engineer User",
                "hash",
                UserRole.ENGINEER,
                true,
                false
        );
    }

    @Test
    @DisplayName("Unauthenticated request to /api/notifications returns 401")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Authenticated user can retrieve recent notifications")
    void testAuthenticatedCanRetrieveNotifications() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createMockPrincipal(userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(),
                userId,
                "Incident Assigned",
                "Assigned to INC-0042",
                NotificationType.INCIDENT_ASSIGNED,
                "INCIDENT",
                "inc-42",
                false,
                Instant.now()
        );

        when(notificationService.getRecentNotifications(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Incident Assigned"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    @DisplayName("Authenticated user can mark all notifications as read")
    void testAuthenticatedCanMarkAllAsRead() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createMockPrincipal(userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(notificationService.markAllAsRead(userId)).thenReturn(4);

        mockMvc.perform(post("/api/notifications/read-all").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(4));
    }
}
