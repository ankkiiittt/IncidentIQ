package com.incidentiq.notification.service;

import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.notification.dto.NotificationResponse;
import com.incidentiq.notification.entity.Notification;
import com.incidentiq.notification.entity.NotificationType;
import com.incidentiq.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create and retrieve notifications for a user")
    void createAndRetrieveNotifications() {
        Notification notification = new Notification(
                userId,
                "Incident Assigned",
                "You have been assigned to INC-0001",
                NotificationType.INCIDENT_ASSIGNED,
                "INCIDENT",
                "inc-1"
        );
        notification.setId(UUID.randomUUID());

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(notification));

        NotificationResponse created = notificationService.createNotification(
                userId,
                "Incident Assigned",
                "You have been assigned to INC-0001",
                NotificationType.INCIDENT_ASSIGNED,
                "INCIDENT",
                "inc-1"
        );

        assertThat(created).isNotNull();
        assertThat(created.title()).isEqualTo("Incident Assigned");
        assertThat(created.read()).isFalse();

        List<NotificationResponse> list = notificationService.getRecentNotifications(userId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).title()).isEqualTo("Incident Assigned");
    }

    @Test
    @DisplayName("Should count unread notifications and mark single as read")
    void unreadCountAndMarkAsRead() {
        UUID notifId = UUID.randomUUID();
        Notification notification = new Notification(
                userId,
                "High Risk Anomaly",
                "Risk score 0.95 detected",
                NotificationType.PREDICTION_ALERT,
                "PREDICTION",
                "pred-1"
        );
        notification.setId(notifId);

        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(3L);
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        long unread = notificationService.getUnreadCount(userId);
        assertThat(unread).isEqualTo(3L);

        NotificationResponse marked = notificationService.markAsRead(notifId, userId);
        assertThat(marked.read()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Should throw FORBIDDEN when user attempts to mark another user's notification")
    void markAsRead_forbidden() {
        UUID notifId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        Notification notification = new Notification(
                otherUser,
                "Title",
                "Message",
                NotificationType.SYSTEM,
                null,
                null
        );
        notification.setId(notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(notifId, userId))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot mark another user's notification");
    }

    @Test
    @DisplayName("Should bulk mark all notifications as read")
    void markAllAsRead() {
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(5);

        int updated = notificationService.markAllAsRead(userId);
        assertThat(updated).isEqualTo(5);
        verify(notificationRepository).markAllAsReadByUserId(userId);
    }
}
