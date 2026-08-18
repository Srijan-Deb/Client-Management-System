package com.cms.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * CMS Notification Service â€” entry point.
 *
 * <p><b>Responsibilities (by build phase):</b>
 * <ul>
 *   <li>Phase 4: Kafka consumers for {@code client-onboarded},
 *       {@code invoice-generated}, {@code payment-success},
 *       {@code payment-failed}, {@code ticket-created}</li>
 *   <li>Renders Thymeleaf email templates and sends via JavaMailSender
 *       (Mailhog in dev, real SMTP in production)</li>
 *   <li>Persists notification records to {@code cms_notification} schema</li>
 * </ul>
 *
 * <p><b>Port:</b> 8084 (see application.yml)
 */
@SpringBootApplication
@EnableKafka
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
