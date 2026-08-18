package com.cms.notification.repository;

import com.cms.notification.domain.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /** Look up the template for a given Kafka event type string. */
    Optional<NotificationTemplate> findByEventType(String eventType);
}
