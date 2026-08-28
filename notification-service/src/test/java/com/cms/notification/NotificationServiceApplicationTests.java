package com.cms.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.kafka.test.context.EmbeddedKafka;

/**
 * Smoke test â€” verifies the Spring context loads without errors.
 */
@SpringBootTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:19999/realms/test",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:19999/realms/test/protocol/openid-connect/certs"
        }
)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:19092", "port=19092" })
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the notification-service context starts without exceptions
    }
}
