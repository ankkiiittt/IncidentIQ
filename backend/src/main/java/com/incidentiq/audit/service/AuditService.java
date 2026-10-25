package com.incidentiq.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.audit.entity.AuditLog;
import com.incidentiq.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String MDC_CORRELATION_ID = "correlationId";

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String action, String entityType, String entityId, Map<String, Object> metadata, String ipAddress) {
        String metadataJson = "{}";
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize audit metadata for action {}: {}", action, e.getMessage());
            }
        }

        String correlationId = MDC.get(MDC_CORRELATION_ID);
        String validIp = (ipAddress != null && (ipAddress.contains(".") || ipAddress.contains(":"))) ? ipAddress : null;
        AuditLog auditLog = new AuditLog(userId, action, entityType, entityId, metadataJson, validIp, correlationId);
        auditLogRepository.save(auditLog);
        log.debug("Audit record created: action={}, user={}, entity={}:{}", action, userId, entityType, entityId);
    }
}
