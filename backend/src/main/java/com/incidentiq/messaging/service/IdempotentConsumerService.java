package com.incidentiq.messaging.service;

import com.incidentiq.messaging.entity.ProcessedEvent;
import com.incidentiq.messaging.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IdempotentConsumerService {

    private static final Logger log = LoggerFactory.getLogger(IdempotentConsumerService.class);

    private final ProcessedEventRepository processedEventRepository;

    public IdempotentConsumerService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean isNewAndMarkProcessed(String consumerName, UUID eventId) {
        if (processedEventRepository.existsByConsumerNameAndEventId(consumerName, eventId)) {
            log.info("Event {} was already processed by consumer '{}'. Skipping duplicate.", eventId, consumerName);
            return false;
        }

        try {
            processedEventRepository.saveAndFlush(new ProcessedEvent(consumerName, eventId));
            log.debug("Marked event {} as processed by consumer '{}'", eventId, consumerName);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Concurrent duplicate detected for event {} by consumer '{}'. Skipping.", eventId, consumerName);
            return false;
        }
    }
}
