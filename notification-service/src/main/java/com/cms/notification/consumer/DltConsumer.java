package com.cms.notification.consumer;

import com.cms.notification.repository.NotificationRepository;
import com.cms.notification.domain.entity.Notification;
import com.cms.notification.domain.enums.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes events from the dead-letter topics ({@code *.DLT}) produced by
 * {@code DefaultErrorHandler} after all retries are exhausted.
 *
 * <p>Persists a {@code FAILED} notification row for every dead-lettered message
 * so the failure is visible in the audit trail â€” nothing is silently lost.
 *
 * <p><b>Interview talking point:</b> DLT consumption is intentionally fire-and-forget
 * (no further retries). Operational runbook: query {@code notifications WHERE status='FAILED'},
 * fix the root cause, then re-publish from the DLT if needed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DltConsumer {

    private final NotificationRepository notificationRepository;

    /**
     * Listens on all {@code *.DLT} topics.
     *
     * <p>Inserts a FAILED notification row so the operations team can see exactly
     * what messages were not delivered and why.
     *
     * @param record the dead-lettered Kafka record (raw payload as String)
     */
    @KafkaListener(
            topicPattern = ".*\\.DLT",
            groupId      = "notification-dlt-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleDeadLetter(ConsumerRecord<String, String> record) {
        String topic   = record.topic();
        String payload = record.value() != null ? record.value() : "<null>";

        log.error("DLT message received from topic={} partition={} offset={} payload={}",
                topic, record.partition(), record.offset(),
                payload.length() > 200 ? payload.substring(0, 200) + "â€¦" : payload);

        // Derive the original event type from the DLT topic name (e.g. "client-onboarded.DLT")
        String originalTopic = topic.endsWith(".DLT")
                ? topic.substring(0, topic.length() - 4)
                : topic;
        String eventType = originalTopic.toUpperCase().replace("-", "_");

        Notification failed = Notification.builder()
                .eventType(eventType)
                .recipientEmail("unknown@dlt")   // payload not parsed; recipient may be embedded
                .subject("[DLT] Undeliverable: " + originalTopic)
                .status(NotificationStatus.FAILED)
                .errorMessage("Dead-lettered after retry exhaustion. Payload: " +
                        (payload.length() > 1000 ? payload.substring(0, 1000) + "â€¦" : payload))
                .build();

        notificationRepository.save(failed);
        log.warn("DLT row persisted: notificationId={} for originalTopic={}",
                failed.getNotificationId(), originalTopic);
    }
}
