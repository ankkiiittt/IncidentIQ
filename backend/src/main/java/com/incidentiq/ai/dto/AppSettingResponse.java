package com.incidentiq.ai.dto;

import com.incidentiq.ai.entity.AppSetting;

import java.time.Instant;

public record AppSettingResponse(
        String key,
        Object value,
        String description,
        Instant updatedAt
) {
    public static AppSettingResponse fromEntity(AppSetting s) {
        return new AppSettingResponse(
                s.getKey(),
                s.getValue(),
                s.getDescription(),
                s.getUpdatedAt()
        );
    }
}
