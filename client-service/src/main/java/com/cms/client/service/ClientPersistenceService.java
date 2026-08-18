package com.cms.client.service;

import com.cms.client.config.AccountServiceClient;
import com.cms.client.domain.entity.ActivityLog;
import com.cms.client.domain.entity.Client;
import com.cms.client.domain.enums.ClientStatus;
import com.cms.client.dto.request.CreateClientRequest;
import com.cms.client.dto.response.ClientResponse;
import com.cms.client.mapper.ClientMapper;
import com.cms.client.repository.ActivityLogRepository;
import com.cms.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the transactional core of client creation.
 *
 * <p><b>Why a separate component?</b> Spring's {@code @Transactional} proxy only
 * intercepts method calls entering from <em>outside</em> the bean. Placing the
 * transactional method in the same class as {@code ClientServiceImpl} (and calling
 * it internally) would bypass the proxy and silently skip transaction management.
 * Extracting it into this dedicated component avoids self-proxy issues cleanly.
 *
 * <p><b>Atomicity guarantee:</b> The Account Service HTTP call is inside
 * the {@code @Transactional} boundary. If it throws, Spring rolls back
 * the client INSERT and activity_log atomically.
 * 503 returned to caller means nothing was persisted â€” retry is safe and idempotent.
 *
 * <p><b>Contacts / Addresses</b> are <em>not</em> created here.
 * They are independent sub-resources managed via their own endpoints:
 * {@code POST /clients/{id}/contacts} and {@code POST /clients/{id}/addresses}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientPersistenceService {

    private final ClientRepository clientRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AccountServiceClient accountServiceClient;
    private final ClientMapper clientMapper;

    /**
     * Persists a new client and all associated data, then provisions an account â€”
     * all within a single transaction. On any failure the entire TX is rolled back.
     *
     * @param req    validated create request
     * @param userId local user_id of the authenticated actor (nullable â€” FK allows NULL)
     * @return fully populated {@link ClientResponse} with account_id set
     */
    @Transactional(rollbackFor = Exception.class)
    public ClientResponse persistNewClient(CreateClientRequest req, Long userId) {

        // â”€â”€ 1. Build and flush client (NULL account_id) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Client client = Client.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .companyName(req.getCompanyName())
                .tier(req.getTier())
                .status(ClientStatus.ACTIVE)
                .createdBy(userId)
                .build();

        // saveAndFlush forces the INSERT + auto-increment assignment NOW,
        // so client_id is available before the Account Service call.
        Client saved = clientRepository.saveAndFlush(client);
        log.debug("Client inserted: clientId={}, email={}", saved.getClientId(), saved.getEmail());

        // â”€â”€ 2. Activity log â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        activityLogRepository.save(ActivityLog.builder()
                .clientId(saved.getClientId())
                .userId(userId)
                .action("CLIENT_CREATED")
                .entityType("CLIENT")
                .entityId(saved.getClientId())
                .description("Client created with email: " + saved.getEmail())
                .build());

        // â”€â”€ 3. Account Service HTTP call â€” INSIDE the transaction â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // ServiceUnavailableException (or any runtime exception) here â†’ full rollback.
        // Circuit breaker: if account-service is OPEN, fallback throws immediately
        // and this transaction rolls back before any DB commit.
        Long accountId = accountServiceClient.linkAccount(
                saved.getClientId(), req.getFirstName(), req.getLastName(), req.getEmail());

        // â”€â”€ 4. Patch account_id â€” JPA dirty-check writes UPDATE before COMMIT â”€â”€â”€
        saved.setAccountId(accountId);
        log.debug("account_id={} patched onto clientId={}", accountId, saved.getClientId());

        // Build and return the response DTO while still inside the TX
        // (ensures lazy collections are accessible via the open session)
        return clientMapper.toResponse(saved);
        // â””â”€ TX commits here on normal return â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    }
}
