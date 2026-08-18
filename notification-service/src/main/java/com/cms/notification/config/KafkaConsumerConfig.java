package com.cms.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.util.Map;

/**
 * Kafka consumer configuration for notification-service.
 *
 * <p><b>Dead-letter / retry strategy (interview talking point):</b>
 * <ul>
 *   <li>Spring Kafka {@link DefaultErrorHandler} retries a failed message up to
 *       <b>3 times with a 2-second fixed back-off</b> before giving up.</li>
 *   <li>After exhaustion, {@link DeadLetterPublishingRecoverer} forwards the message
 *       to {@code {original-topic}.DLT} (e.g. {@code client-onboarded.DLT}).</li>
 *   <li>A separate {@code @KafkaListener} in {@code DltConsumer} drains the DLT,
 *       persists a FAILED notification row, and logs Ã¢â‚¬â€ so <em>nothing is silently lost</em>.</li>
 *   <li>AckMode = MANUAL_IMMEDIATE: offset is committed only after the listener returns
 *       successfully, preventing silent message loss if the process crashes mid-flight.</li>
 * </ul>
 */
@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Producer template used by {@link DeadLetterPublishingRecoverer} to publish
     * unprocessable messages to {@code {topic}.DLT}.
     */
    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        ));
    }

    /**
     * Container factory with:
     * <ul>
     *   <li>MANUAL_IMMEDIATE ack mode (commit only on success)</li>
     *   <li>DefaultErrorHandler: 3 retries Ãƒ- 2s backoff Ã¢â€ â€™ DLT on exhaustion</li>
     * </ul>
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> dltKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(dltKafkaTemplate);

        // FixedBackOff(interval_ms, maxAttempts) Ã¢â‚¬â€ 3 retries, 2s apart
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(2_000L, 3));

        // Log every retry attempt for observability
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Retry attempt={} for topic={} partition={} offset={}: {}",
                        deliveryAttempt,
                        record.topic(), record.partition(), record.offset(),
                        ex.getMessage()));

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setRecordMessageConverter(new StringJsonMessageConverter());
        return factory;
    }
}
