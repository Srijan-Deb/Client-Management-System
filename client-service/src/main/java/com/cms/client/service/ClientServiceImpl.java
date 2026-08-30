package com.cms.client.service;

import java.util.List;
import com.cms.common.exception.ResourceNotFoundException;
import com.cms.client.domain.entity.ActivityLog;
import com.cms.client.domain.entity.Client;
import com.cms.client.dto.request.CreateClientRequest;
import com.cms.client.dto.request.UpdateClientRequest;
import com.cms.client.dto.response.ClientResponse;
import com.cms.client.dto.response.ClientSummaryResponse;
import com.cms.client.dto.response.ActivityLogResponse;
import com.cms.client.mapper.ClientMapper;
import com.cms.client.repository.ActivityLogRepository;
import com.cms.client.repository.ClientRepository;
import com.cms.client.repository.UserProjectionRepository;
import com.cms.common.event.ClientOnboardedEvent;
import com.cms.common.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Non-transactional outer service. Orchestrates:
 * <ol>
 *   <li>Cache-aside email duplicate check</li>
 *   <li>Delegation to {@link ClientPersistenceService} (which owns the TX)</li>
 *   <li>Post-commit side-effects: Redis caching + Kafka event publish</li>
 * </ol>
 *
 * <p>Cache and Kafka are triggered <em>after</em> the transaction commits to
 * prevent publishing events that might be rolled back.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    // â”€â”€ Cache key constants â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    static final String CLIENT_CACHE_PREFIX = "client:";
    static final String EMAIL_CACHE_PREFIX  = "email:";
    static final String EMAIL_EXISTS_VALUE  = "EXISTS";
    static final Duration CACHE_TTL         = Duration.ofMinutes(30);

    private final ClientRepository          clientRepository;
    private final ActivityLogRepository     activityLogRepository;
    private final UserProjectionRepository  userProjectionRepository;
    private final ClientPersistenceService  clientPersistenceService;
    private final ClientMapper              clientMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // â”€â”€ createClient â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public ClientResponse createClient(CreateClientRequest request, Jwt jwt) {
        Long userId = resolveUserId(jwt);
        checkEmailDuplicate(request.getEmail());

        // Transactional: INSERT client + activity_log, HTTP call to Account Service,
        // PATCH account_id â€” all in one TX.
        // Contacts and addresses are managed via their own sub-resource endpoints:
        // POST /clients/{id}/contacts  and  POST /clients/{id}/addresses
        ClientResponse response = clientPersistenceService.persistNewClient(request, userId);
        // TX committed â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

        cacheClient(response);
        cacheEmailExists(response.getEmail());
        publishOnboardedEvent(response);

        return response;
    }

    // â”€â”€ getClientById â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        String key = CLIENT_CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof ClientResponse hit) {
            log.debug("Cache HIT client:{}", id);
            return hit;
        }
        log.debug("Cache MISS client:{}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLIENT_NOT_FOUND", "Client not found with id: " + id));

        ClientResponse response = clientMapper.toResponse(client);
        cacheClient(response);
        return response;
    }

    // â”€â”€ searchClients â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(readOnly = true)
    public Page<ClientSummaryResponse> searchClients(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return clientRepository.findAll(pageable).map(clientMapper::toSummaryResponse);
        }
        return clientRepository.searchByTerm(search.trim(), pageable)
                .map(clientMapper::toSummaryResponse);
    }

    // â”€â”€ updateClient â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional
    public ClientResponse updateClient(Long id, UpdateClientRequest request, Jwt jwt) {
        Long userId = resolveUserId(jwt);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLIENT_NOT_FOUND", "Client not found with id: " + id));

        // Re-run duplicate check if email is being changed
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(client.getEmail())) {
            checkEmailDuplicate(request.getEmail());
            evictEmailCache(client.getEmail()); // evict old email key
        }

        // Apply partial-update fields (null = unchanged)
        if (request.getFirstName()   != null) client.setFirstName(request.getFirstName());
        if (request.getLastName()    != null) client.setLastName(request.getLastName());
        if (request.getEmail()       != null) client.setEmail(request.getEmail());
        if (request.getPhone()       != null) client.setPhone(request.getPhone());
        if (request.getCompanyName() != null) client.setCompanyName(request.getCompanyName());
        if (request.getTier()        != null) client.setTier(request.getTier());
        if (request.getStatus()      != null) client.setStatus(request.getStatus());

        activityLogRepository.save(ActivityLog.builder()
                .clientId(client.getClientId())
                .userId(userId)
                .action("CLIENT_UPDATED")
                .entityType("CLIENT")
                .entityId(client.getClientId())
                .description("Client updated by userId=" + userId)
                .build());

        client = clientRepository.save(client);
        ClientResponse response = clientMapper.toResponse(client);
        // TX commits here — then evict + repopulate cache
        cacheClient(response);
        return response;
    }

    @Override
    public List<ActivityLogResponse> getClientActivityLogs(Long id) {
        // Ensure client exists
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("CLIENT_NOT_FOUND", "Client not found with id: " + id);
        }
        return activityLogRepository.findByClientIdOrderByCreatedAtDesc(id).stream()
                .map(log -> ActivityLogResponse.builder()
                        .logId(log.getLogId())
                        .clientId(log.getClientId())
                        .userId(log.getUserId())
                        .action(log.getAction())
                        .entityType(log.getEntityType())
                        .entityId(log.getEntityId())
                        .description(log.getDescription())
                        .ipAddress(log.getIpAddress())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    // ————————————————————————————————————————————————————————————————————————————————

    /**
     * Cache-aside email duplicate check.
     * Redis hit â†’ instant 409 without touching the DB.
     * Redis miss â†’ DB query, result cached for 30 min.
     */
    private void checkEmailDuplicate(String email) {
        String key = EMAIL_CACHE_PREFIX + email.toLowerCase();
        Object cached = redisTemplate.opsForValue().get(key);
        if (EMAIL_EXISTS_VALUE.equals(cached)) {
            log.debug("Duplicate email detected via Redis: {}", email);
            throw new DuplicateResourceException(
                    "DUPLICATE_EMAIL",
                    "A client with email '" + email + "' already exists");
        }
        // Cache miss â€” check the DB
        if (clientRepository.existsByEmail(email)) {
            // Cache the positive result so subsequent requests hit Redis
            redisTemplate.opsForValue().set(key, EMAIL_EXISTS_VALUE, CACHE_TTL);
            throw new DuplicateResourceException(
                    "DUPLICATE_EMAIL",
                    "A client with email '" + email + "' already exists");
        }
    }

    private void cacheClient(ClientResponse response) {
        String key = CLIENT_CACHE_PREFIX + response.getClientId();
        redisTemplate.opsForValue().set(key, response, CACHE_TTL);
        log.debug("Cached client:{}", response.getClientId());
    }

    private void cacheEmailExists(String email) {
        String key = EMAIL_CACHE_PREFIX + email.toLowerCase();
        redisTemplate.opsForValue().set(key, EMAIL_EXISTS_VALUE, CACHE_TTL);
    }

    private void evictEmailCache(String email) {
        redisTemplate.delete(EMAIL_CACHE_PREFIX + email.toLowerCase());
    }

    private void publishOnboardedEvent(ClientResponse response) {
        ClientOnboardedEvent event = ClientOnboardedEvent.builder()
                .clientId(response.getClientId())
                .accountId(response.getAccountId())
                .email(response.getEmail())
                .firstName(response.getFirstName())
                .lastName(response.getLastName())
                .tier(response.getTier() != null ? response.getTier().name() : null)
                .onboardedAt(Instant.now())
                .build();

        kafkaTemplate.send(ClientOnboardedEvent.TOPIC, String.valueOf(response.getClientId()), event);
        log.info("Published ClientOnboardedEvent for clientId={}", response.getClientId());
    }

    /**
     * Looks up the local {@code user_id} for the authenticated Keycloak principal.
     * Returns {@code null} (acceptable â€” FK is nullable) if the user row is not yet
     * present (should not happen in practice since UserSyncFilter runs first).
     */
    private Long resolveUserId(Jwt jwt) {
        return userProjectionRepository
                .findByKeycloakId(jwt.getSubject())
                .map(u -> u.getUserId())
                .orElse(null);
    }
}
