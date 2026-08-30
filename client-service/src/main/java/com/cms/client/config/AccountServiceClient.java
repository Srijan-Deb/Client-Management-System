package com.cms.client.config;

import com.cms.common.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * HTTP client for synchronous calls to Account Service's account-linking endpoint.
 *
 * <p><b>Circuit breaker ({@code accountService}):</b>
 * <ul>
 *   <li>Opens after 50% failures in a 10-call sliding window (5 out of 10)</li>
 *   <li>Stays open for 10 s, then allows 3 probe calls (HALF_OPEN)</li>
 *   <li>TimeLimiter adds a hard 3 s timeout â€” a slow account-service cannot
 *       hold a client-service thread indefinitely</li>
 *   <li>Fallback method: {@link #linkFallback} â€” throws
 *       {@link ServiceUnavailableException} which propagates through
 *       {@code @Transactional} and triggers a full rollback of the client INSERT</li>
 * </ul>
 *
 * <p><b>Security:</b> The {@code /accounts/link/**} endpoint is permit-all in
 * account-service (Docker network isolation). mTLS / API-key header added in Phase 8.
 *
 * <p><b>Interview talking point:</b> The circuit breaker lives on the <em>caller</em>
 * side (client-service), not the callee. This is the correct placement â€” the caller
 * defends its own thread pool from a failing dependency and fails fast instead of
 * queueing up requests that will all time out anyway.
 */
@Component
@Slf4j
public class AccountServiceClient {

    private final RestClient restClient;

    public AccountServiceClient(
            @Value("${account-service.url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Links (creates) an account for the given client.
     *
     * <p>{@code clientId} is a path variable â€” it is used for logging/correlation
     * on this side and is passed to account-service as a path param only (not stored
     * on the Account entity).
     *
     * @param clientId  client PK â€” path variable on account-service, local logging only
     * @param firstName account name component
     * @param lastName  account name component
     * @param email     contact email
     * @return the generated {@code accountId}
     * @throws ServiceUnavailableException if circuit is open or the call fails
     */
    @CircuitBreaker(name = "accountService", fallbackMethod = "linkFallback")
    public Long linkAccount(Long clientId, String firstName, String lastName, String email) {
        log.debug("Calling Account Service to link account for clientId={}", clientId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/v1/accounts/link/" + clientId)
                    .body(Map.of(
                            "firstName", firstName,
                            "lastName",  lastName,
                            "email",     email
                    ))
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("accountId")) {
                throw new ServiceUnavailableException(
                        "ACCOUNT_SERVICE_INVALID_RESPONSE",
                        "Account Service returned an empty or malformed response");
            }

            Long accountId = ((Number) response.get("accountId")).longValue();
            log.debug("Account linked: accountId={} for clientId={}", accountId, clientId);
            return accountId;

        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Account Service unreachable for clientId={}: {}", clientId, e.getMessage());
            throw new ServiceUnavailableException(
                    "ACCOUNT_SERVICE_UNAVAILABLE",
                    "Account Service is currently unavailable. Please retry.", e);
        } catch (Exception e) {
            log.error("Unexpected error calling Account Service for clientId={}", clientId, e);
            throw new ServiceUnavailableException(
                    "ACCOUNT_SERVICE_ERROR",
                    "Unexpected error communicating with Account Service.", e);
        }
    }

    /**
     * Resilience4j circuit breaker fallback â€” invoked when the circuit is OPEN or
     * the call throws any exception after retry exhaustion.
     *
     * <p>Package-private (not private) so Spring AOP proxies can reflectively
     * invoke this method from the generated subclass.
     *
     * <p>Throws {@link ServiceUnavailableException} so the caller's
     * {@code @Transactional} boundary rolls back the client INSERT atomically.
     */
    // called reflectively by Resilience4j
    Long linkFallback(Long clientId, String firstName, String lastName,
                      String email, Throwable cause) {
        log.error("Circuit breaker OPEN or call failed for clientId={}: {}",
                clientId, cause.getMessage());
        throw new ServiceUnavailableException(
                "ACCOUNT_SERVICE_CIRCUIT_OPEN",
                "Account Service is temporarily unavailable (circuit open). " +
                "Client creation rolled back. Please retry in a few seconds.");
    }
}
