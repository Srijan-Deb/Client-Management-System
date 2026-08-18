package com.cms.notification.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * JPA entity for {@code notification_templates}.
 * One row per Kafka event type â€” seeded in V5 migration.
 */
@Entity
@Table(name = "notification_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    /** Matches the event type string published by Kafka producers (e.g. "CLIENT_ONBOARDED"). */
    @Column(name = "event_type", nullable = false, unique = true, length = 50)
    private String eventType;

    @Column(name = "subject", nullable = false)
    private String subject;

    /** Thymeleaf template filename stem â€” resolved to resources/templates/email/{name}.html */
    @Column(name = "body_template_name", nullable = false, length = 100)
    private String bodyTemplateName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
