package com.incidentiq.messaging.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.messaging.dto.KafkaTopics;
import com.incidentiq.messaging.service.IdempotentConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IncidentLifecycleConsumer {

    private static final Logger log = LoggerFactory.getLogger(IncidentLifecycleConsumer.class);
    private static final String CONSUMER_NAME = "incident-lifecycle-consumer";

    private final IdempotentConsumerService idempotentConsumerService;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final com.incidentiq.ai.service.AiDiagnosticService aiDiagnosticService;
    private final com.incidentiq.ai.service.SettingsService settingsService;

    @org.springframework.beans.factory.annotation.Autowired
    public IncidentLifecycleConsumer(IdempotentConsumerService idempotentConsumerService,
                                     CacheManager cacheManager,
                                     ObjectMapper objectMapper,
                                     @org.springframework.beans.factory.annotation.Autowired(required = false) com.incidentiq.ai.service.AiDiagnosticService aiDiagnosticService,
                                     @org.springframework.beans.factory.annotation.Autowired(required = false) com.incidentiq.ai.service.SettingsService settingsService) {
        this.idempotentConsumerService = idempotentConsumerService;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
        this.aiDiagnosticService = aiDiagnosticService;
        this.settingsService = settingsService;
    }

    public IncidentLifecycleConsumer(IdempotentConsumerService idempotentConsumerService,
                                     CacheManager cacheManager,
                                     ObjectMapper objectMapper) {
        this(idempotentConsumerService, cacheManager, objectMapper, null, null);
    }

    @KafkaListener(
            topics = {KafkaTopics.INCIDENT_CREATED, KafkaTopics.INCIDENT_UPDATED, KafkaTopics.INCIDENT_ESCALATED},
            groupId = "incidentiq-lifecycle-group"
    )
    public void handleIncidentLifecycleEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventIdStr = root.path("eventId").asText();
            String type = root.path("type").asText();

            if (eventIdStr == null || eventIdStr.isBlank()) {
                log.warn("Received event without eventId: {}", message);
                return;
            }

            UUID eventId = UUID.fromString(eventIdStr);

            // 1. Idempotency check
            boolean isNew = idempotentConsumerService.isNewAndMarkProcessed(CONSUMER_NAME, eventId);
            if (!isNew) {
                log.info("Consumer '{}' already processed event {}. Dropping duplicate.", CONSUMER_NAME, eventId);
                return;
            }

            log.info("Processing event '{}' with ID {}", type, eventId);

            // 2. Cache Invalidation on incident lifecycle mutations
            invalidateCaches();

            // 3. Auto-trigger AI triage on incident creation
            if ("incident.created".equals(type) && aiDiagnosticService != null && settingsService != null) {
                if (settingsService.isAutoAnalyzeEnabled()) {
                    String aggIdStr = root.path("aggregateId").asText();
                    if (aggIdStr != null && !aggIdStr.isBlank()) {
                        try {
                            UUID incId = UUID.fromString(aggIdStr);
                            String corrId = root.path("correlationId").asText();
                            log.info("Auto-triggering AI triage analysis for incident {}", incId);
                            aiDiagnosticService.analyzeIncident(incId, com.incidentiq.ai.entity.AnalysisTrigger.AUTO, null, corrId);
                        } catch (Exception ex) {
                            log.warn("Auto-analyze on incident creation failed: {}", ex.getMessage());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to process incident lifecycle event: {}", message, e);
            throw new RuntimeException("Error processing incident lifecycle event", e);
        }
    }

    private void invalidateCaches() {
        try {
            var dashboardCache = cacheManager.getCache("dashboardCache");
            if (dashboardCache != null) {
                dashboardCache.clear();
                log.debug("Invalidated dashboardCache");
            }
            var servicesCache = cacheManager.getCache("servicesCache");
            if (servicesCache != null) {
                servicesCache.clear();
                log.debug("Invalidated servicesCache");
            }
            var analyticsCache = cacheManager.getCache("analyticsCache");
            if (analyticsCache != null) {
                analyticsCache.clear();
                log.debug("Invalidated analyticsCache");
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate cache: {}", e.getMessage());
        }
    }
}
