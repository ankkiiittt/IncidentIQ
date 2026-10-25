package com.incidentiq.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.ai.dto.AppSettingResponse;
import com.incidentiq.ai.entity.AppSetting;
import com.incidentiq.ai.repository.AppSettingRepository;
import com.incidentiq.audit.service.AuditService;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final AppSettingRepository settingRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public SettingsService(AppSettingRepository settingRepository,
                           AuditService auditService,
                           ObjectMapper objectMapper) {
        this.settingRepository = settingRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public boolean isAiEnabled() {
        return getBooleanSetting("ai.enabled", true);
    }

    @Transactional(readOnly = true)
    public boolean isAutoAnalyzeEnabled() {
        return getBooleanSetting("ai.auto_analyze", true);
    }

    @Transactional(readOnly = true)
    public int getRagTopK() {
        return getIntSetting("ai.rag.top_k", 5);
    }

    @Transactional(readOnly = true)
    public double getRagMinSimilarity() {
        return getDoubleSetting("ai.rag.min_similarity", 0.40);
    }

    @Transactional(readOnly = true)
    public int getRateLimitPerHour() {
        return getIntSetting("ai.rate_limit_per_user_per_hour", 10);
    }

    @Transactional(readOnly = true)
    public List<AppSettingResponse> getAllSettings() {
        return settingRepository.findAll().stream()
                .map(AppSettingResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppSettingResponse getSetting(String key) {
        AppSetting s = settingRepository.findByKey(key)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Setting not found: " + key));
        return AppSettingResponse.fromEntity(s);
    }

    @Transactional
    public AppSettingResponse updateSetting(String key, Object value, String description, User actor) {
        com.fasterxml.jackson.databind.JsonNode jsonNode = (value instanceof com.fasterxml.jackson.databind.JsonNode jn)
                ? jn
                : objectMapper.valueToTree(value);

        AppSetting setting = settingRepository.findByKey(key)
                .orElse(new AppSetting(key, jsonNode, description, actor));

        setting.setValue(jsonNode);
        if (description != null) {
            setting.setDescription(description);
        }
        setting.setUpdatedBy(actor);
        setting.setUpdatedAt(Instant.now());

        AppSetting saved = settingRepository.saveAndFlush(setting);
        cache.put(key, jsonNode);

        auditService.record(
                actor != null ? actor.getId() : null,
                "APP_SETTING_UPDATED",
                "AppSetting",
                key,
                Map.of("key", key, "value", String.valueOf(value)),
                "system"
        );

        log.info("App setting '{}' updated by {}", key, actor != null ? actor.getEmail() : "system");
        return AppSettingResponse.fromEntity(saved);
    }

    private boolean getBooleanSetting(String key, boolean defaultValue) {
        Object val = cache.computeIfAbsent(key, k -> settingRepository.findByKey(k)
                .map(AppSetting::getValue)
                .orElse(null));
        if (val == null) return defaultValue;
        if (val instanceof com.fasterxml.jackson.databind.JsonNode jn) return jn.asBoolean(defaultValue);
        if (val instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(val));
    }

    private int getIntSetting(String key, int defaultValue) {
        Object val = cache.computeIfAbsent(key, k -> settingRepository.findByKey(k)
                .map(AppSetting::getValue)
                .orElse(null));
        if (val == null) return defaultValue;
        if (val instanceof com.fasterxml.jackson.databind.JsonNode jn) return jn.asInt(defaultValue);
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double getDoubleSetting(String key, double defaultValue) {
        Object val = cache.computeIfAbsent(key, k -> settingRepository.findByKey(k)
                .map(AppSetting::getValue)
                .orElse(null));
        if (val == null) return defaultValue;
        if (val instanceof com.fasterxml.jackson.databind.JsonNode jn) return jn.asDouble(defaultValue);
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
