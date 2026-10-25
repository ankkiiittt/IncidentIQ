package com.incidentiq.notification.service;

import com.incidentiq.notification.dto.NotificationResponse;
import com.incidentiq.notification.dto.WsEventMessage;
import com.incidentiq.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationBroadcasterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationBroadcaster broadcaster;

    private UUID incidentId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        incidentId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should broadcast incident event to /topic/incidents")
    void broadcastIncident() {
        Map<String, Object> data = Map.of("id", incidentId, "status", "OPEN");
        broadcaster.broadcastIncident(data, "CREATED");

        ArgumentCaptor<WsEventMessage> captor = ArgumentCaptor.forClass(WsEventMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/incidents"), captor.capture());

        WsEventMessage msg = captor.getValue();
        assertThat(msg.eventType()).isEqualTo("INCIDENT");
        assertThat(msg.action()).isEqualTo("CREATED");
        assertThat(msg.data()).isEqualTo(data);
    }

    @Test
    @DisplayName("Should broadcast incident detail event to /topic/incidents/{id}")
    void broadcastIncidentDetail() {
        Map<String, Object> data = Map.of("status", "RESOLVED");
        broadcaster.broadcastIncidentDetail(incidentId, "STATUS_UPDATED", data);

        verify(messagingTemplate).convertAndSend(eq("/topic/incidents/" + incidentId), any(WsEventMessage.class));
    }

    @Test
    @DisplayName("Should broadcast telemetry prediction to /topic/predictions")
    void broadcastPrediction() {
        Map<String, Object> data = Map.of("riskLevel", "HIGH", "score", 0.95);
        broadcaster.broadcastPrediction(data);

        verify(messagingTemplate).convertAndSend(eq("/topic/predictions"), any(WsEventMessage.class));
    }

    @Test
    @DisplayName("Should create notification in DB and broadcast to user queue & topic")
    void sendUserNotification() {
        NotificationResponse mockResp = new NotificationResponse(
                UUID.randomUUID(),
                userId,
                "New Assignment",
                "You have been assigned to an incident",
                NotificationType.INCIDENT_ASSIGNED,
                "INCIDENT",
                incidentId.toString(),
                false,
                Instant.now()
        );

        when(notificationService.createNotification(eq(userId), anyString(), anyString(), any(), any(), any()))
                .thenReturn(mockResp);

        NotificationResponse result = broadcaster.sendUserNotification(
                userId,
                "New Assignment",
                "You have been assigned to an incident",
                NotificationType.INCIDENT_ASSIGNED,
                "INCIDENT",
                incidentId.toString()
        );

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("New Assignment");

        // Verify sent to user's personal queue
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), any(WsEventMessage.class));
        // Verify sent to topic channel
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/" + userId), any(WsEventMessage.class));
    }
}
