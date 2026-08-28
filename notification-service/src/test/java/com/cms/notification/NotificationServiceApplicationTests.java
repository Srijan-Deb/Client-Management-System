package com.cms.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test – verifies the Spring context loads without errors.
 * Uses an in-process Kafka broker on a random port to avoid
 * port conflicts in CI environments.
 */
@SpringBootTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:19999/realms/test",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:19999/realms/test/protocol/openid-connect/certs"
        }
)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1)
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the notification-service context starts without exceptions
    }
}
