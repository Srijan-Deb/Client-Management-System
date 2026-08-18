package com.cms.client.service;

import com.cms.client.dto.request.AddressRequest;
import com.cms.client.dto.response.AddressResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Domain service for managing {@code addresses} sub-resource.
 * <p>Addresses have an independent lifecycle from clients â€” they are created,
 * fetched, and deleted via separate endpoints after the client exists.
 */
public interface AddressService {

    /**
     * Adds a new address to an existing client.
     *
     * @param clientId the owning client's PK
     * @param request  validated address payload
     * @param jwt      authenticated caller (for activity log attribution)
     * @return the persisted address with server-assigned IDs
     * @throws com.cms.common.exception.ResourceNotFoundException if client does not exist
     */
    AddressResponse addAddress(Long clientId, AddressRequest request, Jwt jwt);

    /**
     * Returns all addresses belonging to the given client.
     *
     * @throws com.cms.common.exception.ResourceNotFoundException if client does not exist
     */
    List<AddressResponse> getAddresses(Long clientId);

    /**
     * Deletes an address, verifying it belongs to the given client.
     *
     * @throws com.cms.common.exception.ResourceNotFoundException if address or client not found
     */
    void deleteAddress(Long clientId, Long addressId, Jwt jwt);
}
