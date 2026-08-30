package com.cms.client.service;

import com.cms.client.domain.entity.Client;
import com.cms.client.domain.enums.ClientStatus;
import com.cms.client.domain.enums.ClientTier;
import com.cms.client.dto.request.CreateClientRequest;
import com.cms.client.dto.request.UpdateClientRequest;
import com.cms.client.dto.response.ClientResponse;
import com.cms.client.dto.response.ClientSummaryResponse;
import com.cms.client.mapper.ClientMapper;
import com.cms.client.repository.ActivityLogRepository;
import com.cms.client.repository.ClientRepository;
import com.cms.client.repository.UserProjectionRepository;
import com.cms.common.exception.DuplicateResourceException;
import com.cms.common.exception.ResourceNotFoundException;
import com.cms.common.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// LENIENT: @BeforeEach stubs (jwt, redisTemplate) are not consumed by every test.
// This is intentional â€” setUp provides shared fixtures without duplicating in each test.
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClientServiceImpl â€” Unit Tests")
class ClientServiceImplTest {

    @Mock private ClientRepository          clientRepository;
    @Mock private ActivityLogRepository     activityLogRepository;
    @Mock private UserProjectionRepository  userProjectionRepository;
    @Mock private ClientPersistenceService  clientPersistenceService;
    @Mock private ClientMapper              clientMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;
    @Mock private KafkaTemplate<String, Object>  kafkaTemplate;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Jwt mockJwt;
    private CreateClientRequest createRequest;
    private ClientResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockJwt = mock(Jwt.class);
        when(mockJwt.getSubject()).thenReturn("kc-uuid-123");
        when(userProjectionRepository.findByKeycloakId("kc-uuid-123")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        createRequest = CreateClientRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .tier(ClientTier.STANDARD)
                .build();

        sampleResponse = ClientResponse.builder()
                .clientId(1L)
                .accountId(100L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .tier(ClientTier.STANDARD)
                .status(ClientStatus.ACTIVE)
                .contacts(List.of())
                .addresses(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // â”€â”€ createClient tests â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("createClient: success â€” caches + publishes Kafka event")
    void createClient_success_cachesAndPublishesEvent() {
        when(valueOps.get("email:john@example.com")).thenReturn(null);
        when(clientRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(clientPersistenceService.persistNewClient(eq(createRequest), isNull()))
                .thenReturn(sampleResponse);

        ClientResponse result = clientService.createClient(createRequest, mockJwt);

        assertThat(result.getClientId()).isEqualTo(1L);
        assertThat(result.getAccountId()).isEqualTo(100L);

        // Cache should be populated with client and email
        verify(valueOps).set(eq("client:1"), eq(sampleResponse), any());
        verify(valueOps).set(eq("email:john@example.com"), eq("EXISTS"), any());

        // Kafka event should be published
        verify(kafkaTemplate).send(eq("client-onboarded"), eq("1"), any());
    }

    @Test
    @DisplayName("createClient: duplicate email detected via Redis â€” DB never queried")
    void createClient_duplicateEmail_redisHit_dbNotQueried() {
        when(valueOps.get("email:john@example.com")).thenReturn("EXISTS");

        assertThatThrownBy(() -> clientService.createClient(createRequest, mockJwt))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@example.com");

        verifyNoInteractions(clientRepository);
        verifyNoInteractions(clientPersistenceService);
    }

    @Test
    @DisplayName("createClient: duplicate email found in DB (Redis miss) â€” 409 thrown, cached")
    void createClient_duplicateEmail_dbFallback_cachesAndThrows() {
        when(valueOps.get("email:john@example.com")).thenReturn(null);
        when(clientRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(createRequest, mockJwt))
                .isInstanceOf(DuplicateResourceException.class);

        // Result cached so next request hits Redis
        verify(valueOps).set(eq("email:john@example.com"), eq("EXISTS"), any());
        verifyNoInteractions(clientPersistenceService);
    }

    @Test
    @DisplayName("createClient: Account Service unavailable â€” 503, nothing cached, no Kafka event")
    void createClient_accountServiceFails_propagates503() {
        when(valueOps.get("email:john@example.com")).thenReturn(null);
        when(clientRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(clientPersistenceService.persistNewClient(any(), any()))
                .thenThrow(new ServiceUnavailableException("ACCOUNT_SERVICE_UNAVAILABLE", "down"));

        assertThatThrownBy(() -> clientService.createClient(createRequest, mockJwt))
                .isInstanceOf(ServiceUnavailableException.class);

        // Neither cache nor Kafka should be touched
        verify(valueOps, never()).set(startsWith("client:"), any(), any());
        verifyNoInteractions(kafkaTemplate);
    }

    // â”€â”€ getClientById tests â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("getClientById: cache HIT â€” repository never queried")
    void getClientById_cacheHit_repositoryNotQueried() {
        when(valueOps.get("client:1")).thenReturn(sampleResponse);

        ClientResponse result = clientService.getClientById(1L);

        assertThat(result.getClientId()).isEqualTo(1L);
        verifyNoInteractions(clientRepository);
    }

    @Test
    @DisplayName("getClientById: cache MISS â€” loads from DB, repopulates cache")
    void getClientById_cacheMiss_loadsAndCaches() {
        Client entity = Client.builder().clientId(1L).email("john@example.com")
                .firstName("John").lastName("Doe").tier(ClientTier.STANDARD)
                .status(ClientStatus.ACTIVE).build();

        when(valueOps.get("client:1")).thenReturn(null);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(clientMapper.toResponse(entity)).thenReturn(sampleResponse);

        ClientResponse result = clientService.getClientById(1L);

        assertThat(result.getClientId()).isEqualTo(1L);
        verify(valueOps).set(eq("client:1"), eq(sampleResponse), any());
    }

    @Test
    @DisplayName("getClientById: not found â€” 404 thrown")
    void getClientById_notFound_throws404() {
        when(valueOps.get("client:99")).thenReturn(null);
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // â”€â”€ searchClients test â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("searchClients: no Redis interaction â€” always queries DB")
    void searchClients_noCaching() {
        Client entity = Client.builder().clientId(1L).email("john@example.com")
                .firstName("John").lastName("Doe").tier(ClientTier.STANDARD)
                .status(ClientStatus.ACTIVE).build();
        ClientSummaryResponse summary = ClientSummaryResponse.builder().clientId(1L).build();
        PageRequest pageable = PageRequest.of(0, 20);

        when(clientRepository.searchByTerm("John", pageable))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(clientMapper.toSummaryResponse(entity)).thenReturn(summary);

        var result = clientService.searchClients("John", pageable);

        assertThat(result.getContent()).hasSize(1);
        // No Redis interactions for search
        verify(valueOps, never()).set(any(), any(), any());
    }

    // â”€â”€ updateClient test â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("updateClient: cache evicted and repopulated after save")
    void updateClient_evictsAndRepopulatesCache() {
        Client entity = Client.builder().clientId(1L).email("john@example.com")
                .firstName("John").lastName("Doe").tier(ClientTier.STANDARD)
                .status(ClientStatus.ACTIVE).build();
        UpdateClientRequest req = UpdateClientRequest.builder()
                .firstName("Johnny").tier(ClientTier.PREMIUM).build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(clientRepository.save(entity)).thenReturn(entity);
        when(clientMapper.toResponse(entity)).thenReturn(sampleResponse);

        ClientResponse result = clientService.updateClient(1L, req, mockJwt);

        assertThat(result).isNotNull();
        // Cache should be updated with fresh response
        verify(valueOps).set(eq("client:1"), eq(sampleResponse), any());
    }
}
