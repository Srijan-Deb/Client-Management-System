package com.cms.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * CMS Client Service â€” entry point.
 *
 * <p><b>Responsibilities (by build phase):</b>
 * <ul>
 *   <li>Phase 2: Client onboarding (POST /clients), Redis duplicate-email cache,
 *       publishes {@code ClientOnboardedEvent} to Kafka</li>
 *   <li>Phase 3: Calls Account Service synchronously to link a new account</li>
 *   <li>Phase 6: Support ticket module (support_tickets, ticket_comments tables)</li>
 * </ul>
 *
 * <p><b>Port:</b> 8081 (see application.yml)
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
public class ClientServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientServiceApplication.class, args);
    }
}
