package com.incidentiq.notification.dto;

import java.time.Instant;

public record WsEventMessage(
        String eventType,
        String action,
        Instant timestamp,
        Object data
) {
    public static WsEventMessage of(String eventType, String action, Object data) {
        return new WsEventMessage(eventType, action, Instant.now(), data);
    }
}
