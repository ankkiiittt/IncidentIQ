package com.incidentiq.messaging.service;

import com.incidentiq.messaging.dto.KafkaTopics;
import com.incidentiq.messaging.entity.OutboxEvent;
import com.incidentiq.messaging.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate);
    }

    @Test
    @DisplayName("publishPendingEvents forwards events to Kafka and sets published_at timestamp")
    void testSuccessfulPublish() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(eventId, "INCIDENT", aggregateId, KafkaTopics.INCIDENT_CREATED, "{}", "cid-1");

        when(outboxEventRepository.findUnpublishedForUpdate(50)).thenReturn(List.of(event));

        RecordMetadata metadata = new RecordMetadata(new TopicPartition(KafkaTopics.INCIDENT_CREATED, 0), 0, 0, 0, 0, 0);
        SendResult<String, String> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(eq(KafkaTopics.INCIDENT_CREATED), eq(aggregateId.toString()), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        outboxPublisher.publishPendingEvents();

        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("publishPendingEvents handles Kafka failure gracefully without setting published_at")
    void testBrokerFailureLeavesEventUnpublished() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(eventId, "INCIDENT", aggregateId, KafkaTopics.INCIDENT_CREATED, "{}", "cid-1");

        when(outboxEventRepository.findUnpublishedForUpdate(50)).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Connection to broker failed"));
        when(kafkaTemplate.send(eq(KafkaTopics.INCIDENT_CREATED), eq(aggregateId.toString()), any(String.class)))
                .thenReturn(failedFuture);

        outboxPublisher.publishPendingEvents();

        assertThat(event.getPublishedAt()).isNull();
        verify(outboxEventRepository, never()).save(event);
    }
}
