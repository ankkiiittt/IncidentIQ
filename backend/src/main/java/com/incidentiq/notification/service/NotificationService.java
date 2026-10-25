package com.incidentiq.notification.service;

import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.notification.dto.NotificationResponse;
import com.incidentiq.notification.entity.Notification;
import com.incidentiq.notification.entity.NotificationType;
import com.incidentiq.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(UUID userId) {
        return notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public NotificationResponse markAsRead(UUID id, UUID userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Cannot mark another user's notification as read");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }

    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    public NotificationResponse createNotification(UUID userId,
                                                  String title,
                                                  String message,
                                                  NotificationType type,
                                                  String entityType,
                                                  String entityId) {
        Notification notification = new Notification(userId, title, message, type, entityType, entityId);
        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }
}
