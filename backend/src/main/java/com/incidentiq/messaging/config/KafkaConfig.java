package com.incidentiq.messaging.config;

import com.incidentiq.messaging.dto.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + KafkaTopics.DLT_SUFFIX, record.partition())
        );

        // 3 total attempts: 1 initial + 2 retries with 500ms backoff
        FixedBackOff backOff = new FixedBackOff(500L, 2L);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public NewTopic incidentCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.INCIDENT_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic incidentUpdatedTopic() {
        return TopicBuilder.name(KafkaTopics.INCIDENT_UPDATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic incidentEscalatedTopic() {
        return TopicBuilder.name(KafkaTopics.INCIDENT_ESCALATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic incidentCreatedDltTopic() {
        return TopicBuilder.name(KafkaTopics.INCIDENT_CREATED + KafkaTopics.DLT_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic incidentUpdatedDltTopic() {
        return TopicBuilder.name(KafkaTopics.INCIDENT_UPDATED + KafkaTopics.DLT_SUFFIX).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic incidentEscalatedDltTopic() {
        return TopicBuilder.name(KafkaTopics.INCIDENT_ESCALATED + KafkaTopics.DLT_SUFFIX).partitions(1).replicas(1).build();
    }
}
