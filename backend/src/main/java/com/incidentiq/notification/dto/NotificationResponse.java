package com.incidentiq.notification.dto;

import com.incidentiq.notification.entity.Notification;
import com.incidentiq.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String title,
        String message,
        NotificationType type,
        String entityType,
        String entityId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse fromEntity(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getEntityType(),
                n.getEntityId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
