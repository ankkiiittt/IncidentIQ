package com.incidentiq.messaging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.messaging.dto.IncidentCreatedPayload;
import com.incidentiq.messaging.dto.KafkaTopics;
import com.incidentiq.messaging.entity.OutboxEvent;
import com.incidentiq.messaging.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        outboxService = new OutboxService(outboxEventRepository, objectMapper);
    }

    @Test
    @DisplayName("publish records event envelope in outbox_events table")
    void testPublishEvent() {
        UUID incidentId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();

        IncidentCreatedPayload payload = new IncidentCreatedPayload(
                incidentId, serviceId, "P1", "CRITICAL", reporterId
        );

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArgument(0));

        OutboxEvent result = outboxService.publish("INCIDENT", incidentId, KafkaTopics.INCIDENT_CREATED, payload);

        assertThat(result).isNotNull();
        assertThat(result.getAggregateType()).isEqualTo("INCIDENT");
        assertThat(result.getAggregateId()).isEqualTo(incidentId);
        assertThat(result.getEventType()).isEqualTo(KafkaTopics.INCIDENT_CREATED);
        assertThat(result.getPublishedAt()).isNull();
        assertThat(result.getPayload()).contains("incident.created");
        assertThat(result.getPayload()).contains(incidentId.toString());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isNotNull();
    }
}
