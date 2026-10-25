package com.incidentiq.notification.controller;

import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.notification.dto.NotificationResponse;
import com.incidentiq.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Real-time user alerts and notification history")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get recent notifications for current user")
    public ResponseEntity<List<NotificationResponse>> getRecentNotifications(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(notificationService.getRecentNotifications(currentUser.getId()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUser.getId()));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read for current user")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        int count = notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(Map.of("updated", count, "message", "All notifications marked as read"));
    }
}
