package com.incidentiq.messaging.service;

import com.incidentiq.messaging.entity.OutboxEvent;
import com.incidentiq.messaging.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${incidentiq.outbox.interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findUnpublishedForUpdate(50);
        if (pending.isEmpty()) {
            return;
        }

        log.debug("Found {} unpublished outbox events to relay to Kafka", pending.size());

        for (OutboxEvent event : pending) {
            try {
                // Send to Kafka with aggregateId as key for partition affinity
                kafkaTemplate.send(
                        event.getEventType(),
                        event.getAggregateId().toString(),
                        event.getPayload()
                ).get(3, TimeUnit.SECONDS);

                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
                log.info("Relayed outbox event {} ({}) to Kafka", event.getEventId(), event.getEventType());
            } catch (Exception e) {
                log.warn("Failed to publish outbox event {} ({}) to Kafka broker: {}. Will retry on next poll.",
                        event.getEventId(), event.getEventType(), e.getMessage());
                // Break loop so subsequent events maintain order
                break;
            }
        }
    }
}
