package com.cms.gateway.config;

import com.cms.common.security.CmsJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway-level security configuration.
 *
 * <p>Validates JWTs at the gateway boundary before forwarding requests
 * to backend services. Backend services also validate independently
 * (Defence-in-depth â€” Phase 1, Decision 1).
 *
 * <p>The gateway does NOT add user/role headers â€” backend services
 * read the JWT directly from the Authorization header.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain gatewaySecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Gateway health and info always permitted
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Keycloak token endpoint proxied through gateway (optional convenience)
                        .pathMatchers(HttpMethod.POST, "/realms/*/protocol/openid-connect/token").permitAll()
                        // Allow CORS preflight requests
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        // All other routes require a valid JWT
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                // Reactive wrapper â€” CmsJwtAuthenticationConverter returns
                                // AbstractAuthenticationToken; gateway needs ReactiveJwtAuthConverter
                                source -> reactor.core.publisher.Mono.just(
                                        new CmsJwtAuthenticationConverter().convert(source))
                        ))
                )
                .build();
    }
}
