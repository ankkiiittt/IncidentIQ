package com.incidentiq.messaging.service;

import com.incidentiq.messaging.entity.ProcessedEvent;
import com.incidentiq.messaging.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotentConsumerServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private IdempotentConsumerService idempotentConsumerService;

    @BeforeEach
    void setUp() {
        idempotentConsumerService = new IdempotentConsumerService(processedEventRepository);
    }

    @Test
    @DisplayName("isNewAndMarkProcessed returns true for first delivery and saves record")
    void testFirstDeliveryReturnsTrue() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsByConsumerNameAndEventId("test-consumer", eventId)).thenReturn(false);

        boolean isNew = idempotentConsumerService.isNewAndMarkProcessed("test-consumer", eventId);

        assertThat(isNew).isTrue();
        verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("isNewAndMarkProcessed returns false for duplicate delivery without re-saving")
    void testDuplicateDeliveryReturnsFalse() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsByConsumerNameAndEventId("test-consumer", eventId)).thenReturn(true);

        boolean isNew = idempotentConsumerService.isNewAndMarkProcessed("test-consumer", eventId);

        assertThat(isNew).isFalse();
        verify(processedEventRepository, never()).saveAndFlush(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("isNewAndMarkProcessed handles concurrent race condition via DataIntegrityViolationException")
    void testConcurrentRaceConditionReturnsFalse() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsByConsumerNameAndEventId("test-consumer", eventId)).thenReturn(false);
        when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        boolean isNew = idempotentConsumerService.isNewAndMarkProcessed("test-consumer", eventId);

        assertThat(isNew).isFalse();
    }
}
