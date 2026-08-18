package com.cms.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate limiter configuration for the CMS API Gateway (Phase 8).
 *
 * <p>Uses Spring Cloud Gateway's built-in {@code RequestRateLimiter} filter backed by
 * Redis (token-bucket algorithm). Limits are applied <em>per authenticated user</em>
 * (JWT {@code sub} claim) Ã¢â‚¬â€ anonymous requests fall back to the client IP so that
 * unauthenticated probing is also rate-limited.
 *
 * <p>Configured values (tunable via application.yml):
 * <ul>
 *   <li>replenishRate  = 20 req/s Ã¢â‚¬â€ sustained throughput per user</li>
 *   <li>burstCapacity  = 40       Ã¢â‚¬â€ short-spike allowance (2Ãƒ- sustained)</li>
 *   <li>requestedTokens = 1       Ã¢â‚¬â€ each request costs 1 token</li>
 * </ul>
 *
 * <p>When the bucket is empty the gateway returns {@code 429 Too Many Requests}
 * with the standard {@code X-RateLimit-*} headers so clients can back off.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the rate-limit key for each incoming request.
     *
     * <ul>
     *   <li>Authenticated requests: key = JWT {@code sub} claim (Keycloak user ID).
     *       This ensures limits are per-user regardless of IP (e.g. behind a NAT).</li>
     *   <li>Unauthenticated requests: key = remote IP address as a fallback so that
     *       pre-auth endpoints (e.g. /actuator/health) are still protected.</li>
     * </ul>
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> "user:" + principal.getName())
                .switchIfEmpty(
                        Mono.just(
                                "ip:" + exchange.getRequest()
                                        .getRemoteAddress()
                                        .getAddress()
                                        .getHostAddress()
                        )
                );
    }

    /**
     * Resolves the rate-limit key strictly by IP address.
     * Used for login/token endpoints where there is no user principal yet.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                "ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}
