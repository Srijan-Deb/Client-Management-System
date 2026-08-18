package com.cms.client.service;

import com.cms.client.domain.entity.ActivityLog;
import com.cms.client.domain.entity.Address;
import com.cms.client.dto.request.AddressRequest;
import com.cms.client.dto.response.AddressResponse;
import com.cms.client.mapper.ClientMapper;
import com.cms.client.repository.ActivityLogRepository;
import com.cms.client.repository.AddressRepository;
import com.cms.client.repository.ClientRepository;
import com.cms.client.repository.UserProjectionRepository;
import com.cms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link AddressService}.
 *
 * <p>Each mutating operation:
 * <ol>
 *   <li>Verifies the parent client exists.</li>
 *   <li>Persists the address (or deletes it).</li>
 *   <li>Appends an activity_log entry.</li>
 *   <li>Evicts the {@code client:{id}} Redis cache so the next GET sees fresh data.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressRepository         addressRepository;
    private final ClientRepository          clientRepository;
    private final ActivityLogRepository     activityLogRepository;
    private final UserProjectionRepository  userProjectionRepository;
    private final ClientMapper              clientMapper;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    // â”€â”€ addAddress â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressResponse addAddress(Long clientId, AddressRequest request, Jwt jwt) {
        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLIENT_NOT_FOUND", "Client not found: " + clientId));

        Address address = clientMapper.toAddressEntity(request);
        address.setClient(client);
        Address saved = addressRepository.save(address);

        Long userId = resolveUserId(jwt);
        activityLogRepository.save(ActivityLog.builder()
                .clientId(clientId)
                .userId(userId)
                .action("ADDRESS_ADDED")
                .entityType("ADDRESS")
                .entityId(saved.getAddressId())
                .description("Address added: " + saved.getLine1() + ", " + saved.getCity())
                .build());

        evictClientCache(clientId);
        log.debug("Address {} added to clientId={}", saved.getAddressId(), clientId);
        return clientMapper.toAddressResponse(saved);
    }

    // â”€â”€ getAddresses â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public List<AddressResponse> getAddresses(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("CLIENT_NOT_FOUND", "Client not found: " + clientId);
        }
        return addressRepository.findByClientClientId(clientId)
                .stream()
                .map(clientMapper::toAddressResponse)
                .toList();
    }

    // â”€â”€ deleteAddress â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long clientId, Long addressId, Jwt jwt) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("CLIENT_NOT_FOUND", "Client not found: " + clientId);
        }
        if (!addressRepository.existsByAddressIdAndClientClientId(addressId, clientId)) {
            throw new ResourceNotFoundException("ADDRESS_NOT_FOUND",
                    "Address " + addressId + " not found for client " + clientId);
        }

        addressRepository.deleteById(addressId);

        Long userId = resolveUserId(jwt);
        activityLogRepository.save(ActivityLog.builder()
                .clientId(clientId)
                .userId(userId)
                .action("ADDRESS_DELETED")
                .entityType("ADDRESS")
                .entityId(addressId)
                .description("Address " + addressId + " deleted")
                .build());

        evictClientCache(clientId);
        log.debug("Address {} deleted from clientId={}", addressId, clientId);
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Long resolveUserId(Jwt jwt) {
        if (jwt == null) return null;
        return userProjectionRepository.findByKeycloakId(jwt.getSubject())
                .map(u -> u.getUserId())
                .orElse(null);
    }

    private void evictClientCache(Long clientId) {
        redisTemplate.ifPresent(rt -> {
            rt.delete("client:" + clientId);
            log.debug("Evicted client:{} from cache after address mutation", clientId);
        });
    }
}
