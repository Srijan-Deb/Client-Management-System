package com.cms.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test â€” verifies the Spring context loads without errors.
 * Uses a random port and disables Keycloak connectivity requirement
 * by overriding the issuer-uri with a dummy value.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:19999/realms/test",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:19999/realms/test/protocol/openid-connect/certs"
        }
)
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the gateway context starts without exceptions
    }
}
