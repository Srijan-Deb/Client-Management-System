package com.cms.client.service;

import com.cms.client.dto.request.ContactRequest;
import com.cms.client.dto.response.ContactResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Domain service for managing {@code contacts} sub-resource.
 * <p>Contacts have an independent lifecycle from clients â€” they are created,
 * fetched, and deleted via separate endpoints after the client exists.
 */
public interface ContactService {

    /**
     * Adds a new contact to an existing client.
     *
     * @param clientId the owning client's PK
     * @param request  validated contact payload
     * @param jwt      authenticated caller (for activity log attribution)
     * @return the persisted contact with server-assigned IDs
     * @throws com.cms.common.exception.ResourceNotFoundException if client does not exist
     */
    ContactResponse addContact(Long clientId, ContactRequest request, Jwt jwt);

    /**
     * Returns all contacts belonging to the given client.
     *
     * @throws com.cms.common.exception.ResourceNotFoundException if client does not exist
     */
    List<ContactResponse> getContacts(Long clientId);

    /**
     * Deletes a contact, verifying it belongs to the given client.
     *
     * @throws com.cms.common.exception.ResourceNotFoundException if contact or client not found
     */
    void deleteContact(Long clientId, Long contactId, Jwt jwt);
}
