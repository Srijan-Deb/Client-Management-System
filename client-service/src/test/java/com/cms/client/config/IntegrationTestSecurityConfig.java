package com.cms.client.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test-only Security configuration â€” active only under the {@code integration-test}
 * Spring profile.
 *
 * <p>Replaces the production {@link com.cms.client.config.SecurityConfig} with a
 * permit-all chain so integration tests can send unauthenticated HTTP requests without
 * embedding a full JWT or standing up Keycloak.
 *
 * <p><b>Why @Order(1)?</b> Spring Security evaluates {@link SecurityFilterChain} beans
 * in ascending order. {@code @Order(1)} ensures this chain is checked first and
 * matches every request before the production chain (which defaults to order 100)
 * gets a chance to demand authentication.
 *
 * <p><b>Why @TestConfiguration + @Profile?</b>
 * <ul>
 *   <li>{@code @TestConfiguration} is only scanned when the test application context
 *       is bootstrapped â€” it has no effect on the production binary.</li>
 *   <li>{@code @Profile("integration-test")} means the bean only registers when
 *       {@code @ActiveProfiles("integration-test")} is present on the test class,
 *       so other test classes (smoke tests, unit tests) are unaffected.</li>
 * </ul>
 */
@TestConfiguration
@Profile("integration-test")
public class IntegrationTestSecurityConfig {

    /**
     * Permit-all security chain â€” bypasses JWT authentication for integration tests.
     *
     * <p>CSRF is disabled (standard for stateless REST APIs).
     * All requests are permitted without any authentication or authorisation check.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain integrationTestFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
