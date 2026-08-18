package com.cms.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test â€” verifies the Spring context loads without errors.
 * Disables Keycloak/Redis/Kafka/MySQL connectivity by overriding
 * auto-configuration and using embedded alternatives where possible.
 *
 * <p>{@code @MockBean} for {@code RedisTemplate} and {@code KafkaTemplate} satisfies
 * the autowiring requirements of {@code ClientServiceImpl} when the real
 * auto-configurations are excluded.
 */
@SpringBootTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MYSQL",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.cache.type=simple",
                "spring.kafka.bootstrap-servers=localhost:19092",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:19999/realms/test",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:19999/realms/test/protocol/openid-connect/certs",
                // Default URL so AccountServiceClient @Value doesn't fail (no connection made at startup)
                "account-service.url=http://localhost:19999"
        }
)
@ActiveProfiles("test")
class ClientServiceApplicationTests {

    /** Satisfies ClientServiceImpl autowiring when RedisAutoConfiguration is excluded. */
    @MockBean
    @SuppressWarnings("rawtypes")
    RedisTemplate redisTemplate;

    /** Satisfies ClientServiceImpl autowiring when KafkaAutoConfiguration is excluded. */
    @MockBean
    @SuppressWarnings("rawtypes")
    KafkaTemplate kafkaTemplate;

    @Test
    void contextLoads() {
        // Verifies the client-service context starts without exceptions
    }
}
