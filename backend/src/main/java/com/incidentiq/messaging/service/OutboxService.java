package com.incidentiq.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.common.filter.CorrelationIdFilter;
import com.incidentiq.messaging.dto.EventEnvelope;
import com.incidentiq.messaging.entity.OutboxEvent;
import com.incidentiq.messaging.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publish(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        EventEnvelope<Object> envelope = EventEnvelope.of(eventType, aggregateId, correlationId, payload);

        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope);
            OutboxEvent event = new OutboxEvent(
                    envelope.eventId(),
                    aggregateType,
                    aggregateId,
                    eventType,
                    jsonPayload,
                    correlationId
            );

            OutboxEvent saved = outboxEventRepository.save(event);
            log.debug("Outbox event recorded: type={}, id={}, aggregateId={}", eventType, envelope.eventId(), aggregateId);
            return saved;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for type {}", eventType, e);
            throw new IllegalStateException("Could not serialize outbox payload", e);
        }
    }
}
