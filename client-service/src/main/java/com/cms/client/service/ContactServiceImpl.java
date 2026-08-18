package com.cms.client.service;

import com.cms.client.domain.entity.ActivityLog;
import com.cms.client.domain.entity.Contact;
import com.cms.client.dto.request.ContactRequest;
import com.cms.client.dto.response.ContactResponse;
import com.cms.client.mapper.ClientMapper;
import com.cms.client.repository.ActivityLogRepository;
import com.cms.client.repository.ClientRepository;
import com.cms.client.repository.ContactRepository;
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
 * Implementation of {@link ContactService}.
 *
 * <p>Each mutating operation:
 * <ol>
 *   <li>Verifies the parent client exists.</li>
 *   <li>Persists the contact (or deletes it).</li>
 *   <li>Appends an activity_log entry.</li>
 *   <li>Evicts the {@code client:{id}} Redis cache so the next GET sees fresh data.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final ContactRepository         contactRepository;
    private final ClientRepository          clientRepository;
    private final ActivityLogRepository     activityLogRepository;
    private final UserProjectionRepository  userProjectionRepository;
    private final ClientMapper              clientMapper;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    // â”€â”€ addContact â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContactResponse addContact(Long clientId, ContactRequest request, Jwt jwt) {
        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLIENT_NOT_FOUND", "Client not found: " + clientId));

        Contact contact = clientMapper.toContactEntity(request);
        contact.setClient(client);
        Contact saved = contactRepository.save(contact);

        Long userId = resolveUserId(jwt);
        activityLogRepository.save(ActivityLog.builder()
                .clientId(clientId)
                .userId(userId)
                .action("CONTACT_ADDED")
                .entityType("CONTACT")
                .entityId(saved.getContactId())
                .description("Contact added: " + saved.getFirstName() + " " + saved.getLastName())
                .build());

        evictClientCache(clientId);
        log.debug("Contact {} added to clientId={}", saved.getContactId(), clientId);
        return clientMapper.toContactResponse(saved);
    }

    // â”€â”€ getContacts â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public List<ContactResponse> getContacts(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("CLIENT_NOT_FOUND", "Client not found: " + clientId);
        }
        return contactRepository.findByClientClientId(clientId)
                .stream()
                .map(clientMapper::toContactResponse)
                .toList();
    }

    // â”€â”€ deleteContact â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContact(Long clientId, Long contactId, Jwt jwt) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("CLIENT_NOT_FOUND", "Client not found: " + clientId);
        }
        if (!contactRepository.existsByContactIdAndClientClientId(contactId, clientId)) {
            throw new ResourceNotFoundException("CONTACT_NOT_FOUND",
                    "Contact " + contactId + " not found for client " + clientId);
        }

        contactRepository.deleteById(contactId);

        Long userId = resolveUserId(jwt);
        activityLogRepository.save(ActivityLog.builder()
                .clientId(clientId)
                .userId(userId)
                .action("CONTACT_DELETED")
                .entityType("CONTACT")
                .entityId(contactId)
                .description("Contact " + contactId + " deleted")
                .build());

        evictClientCache(clientId);
        log.debug("Contact {} deleted from clientId={}", contactId, clientId);
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
            log.debug("Evicted client:{} from cache after contact mutation", clientId);
        });
    }
}
