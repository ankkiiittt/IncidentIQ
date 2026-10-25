package com.incidentiq.notification.service;

import com.incidentiq.notification.dto.NotificationResponse;
import com.incidentiq.notification.dto.WsEventMessage;
import com.incidentiq.notification.entity.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public NotificationBroadcaster(SimpMessagingTemplate messagingTemplate,
                                   NotificationService notificationService) {
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    /**
     * Broadcasts general incident state changes to /topic/incidents
     */
    public void broadcastIncident(Object incidentData, String action) {
        try {
            WsEventMessage message = WsEventMessage.of("INCIDENT", action, incidentData);
            messagingTemplate.convertAndSend("/topic/incidents", message);
            log.debug("Broadcast incident event to /topic/incidents: action={}", action);
        } catch (Exception e) {
            log.warn("Failed to broadcast to /topic/incidents: {}", e.getMessage());
        }
    }

    /**
     * Broadcasts specific incident updates (timeline, comment, status, AI) to /topic/incidents/{incidentId}
     */
    public void broadcastIncidentDetail(UUID incidentId, String action, Object data) {
        try {
            WsEventMessage message = WsEventMessage.of("INCIDENT_DETAIL", action, data);
            messagingTemplate.convertAndSend("/topic/incidents/" + incidentId, message);
            log.debug("Broadcast to /topic/incidents/{}: action={}", incidentId, action);
        } catch (Exception e) {
            log.warn("Failed to broadcast to /topic/incidents/{}: {}", incidentId, e.getMessage());
        }
    }

    /**
     * Broadcasts telemetry anomaly predictions to /topic/predictions
     */
    public void broadcastPrediction(Object predictionData) {
        try {
            WsEventMessage message = WsEventMessage.of("PREDICTION", "CREATED", predictionData);
            messagingTemplate.convertAndSend("/topic/predictions", message);
            log.debug("Broadcast prediction event to /topic/predictions");
        } catch (Exception e) {
            log.warn("Failed to broadcast to /topic/predictions: {}", e.getMessage());
        }
    }

    /**
     * Sends targeted notification to a specific user (saved to DB and pushed via WebSocket)
     */
    public NotificationResponse sendUserNotification(UUID userId,
                                                     String title,
                                                     String messageText,
                                                     NotificationType type,
                                                     String entityType,
                                                     String entityId) {
        NotificationResponse notification = notificationService.createNotification(
                userId, title, messageText, type, entityType, entityId
        );

        try {
            WsEventMessage wsMessage = WsEventMessage.of("NOTIFICATION", "CREATED", notification);
            // Send to user's personal queue /user/queue/notifications
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", wsMessage);
            // Also send to topic channel for flexible frontend consumption
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, wsMessage);
            log.debug("Sent user notification to user {}: {}", userId, title);
        } catch (Exception e) {
            log.warn("Failed to push real-time user notification to {}: {}", userId, e.getMessage());
        }

        return notification;
    }
}
