package com.incidentiq.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.messaging.dto.EventEnvelope;
import com.incidentiq.messaging.dto.IncidentCreatedPayload;
import com.incidentiq.messaging.dto.KafkaTopics;
import com.incidentiq.messaging.service.IdempotentConsumerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentLifecycleConsumerTest {

    @Mock
    private IdempotentConsumerService idempotentConsumerService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache dashboardCache;

    @Mock
    private Cache servicesCache;

    private ObjectMapper objectMapper;
    private IncidentLifecycleConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new IncidentLifecycleConsumer(idempotentConsumerService, cacheManager, objectMapper);
    }

    @Test
    @DisplayName("First delivery of incident event invalidates dashboard and service caches")
    void testFirstDeliveryInvalidatesCaches() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        EventEnvelope<IncidentCreatedPayload> envelope = EventEnvelope.of(
                KafkaTopics.INCIDENT_CREATED,
                incidentId,
                "cid-1",
                new IncidentCreatedPayload(incidentId, UUID.randomUUID(), "P2", "HIGH", UUID.randomUUID())
        );

        when(idempotentConsumerService.isNewAndMarkProcessed(eq("incident-lifecycle-consumer"), eq(envelope.eventId())))
                .thenReturn(true);
        when(cacheManager.getCache("dashboardCache")).thenReturn(dashboardCache);
        when(cacheManager.getCache("servicesCache")).thenReturn(servicesCache);

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handleIncidentLifecycleEvent(json);

        verify(dashboardCache).clear();
        verify(servicesCache).clear();
    }

    @Test
    @DisplayName("Duplicate event delivery does not invalidate caches or re-process")
    void testDuplicateDeliverySkipped() throws Exception {
        UUID incidentId = UUID.randomUUID();
        EventEnvelope<IncidentCreatedPayload> envelope = EventEnvelope.of(
                KafkaTopics.INCIDENT_CREATED,
                incidentId,
                "cid-1",
                new IncidentCreatedPayload(incidentId, UUID.randomUUID(), "P2", "HIGH", UUID.randomUUID())
        );

        when(idempotentConsumerService.isNewAndMarkProcessed(eq("incident-lifecycle-consumer"), eq(envelope.eventId())))
                .thenReturn(false);

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handleIncidentLifecycleEvent(json);

        verify(dashboardCache, never()).clear();
        verify(servicesCache, never()).clear();
    }
}
