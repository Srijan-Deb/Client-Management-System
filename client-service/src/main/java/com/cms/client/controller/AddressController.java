package com.cms.client.controller;

import com.cms.client.dto.request.AddressRequest;
import com.cms.client.dto.response.AddressResponse;
import com.cms.client.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for the {@code addresses} sub-resource.
 *
 * <p>Base path: {@code /api/v1/clients/{clientId}/addresses}
 *
 * <p>Addresses have an independent lifecycle from the parent client:
 * the client must exist before addresses can be added.
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * Add an address to an existing client.
     * <p>{@code POST /api/v1/clients/{clientId}/addresses} â†’ 201 Created
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager')")
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable Long clientId,
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        AddressResponse response = addressService.addAddress(clientId, request, jwt);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{addressId}")
                .buildAndExpand(response.getAddressId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * List all addresses for a client.
     * <p>{@code GET /api/v1/clients/{clientId}/addresses} â†’ 200 OK
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent')")
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable Long clientId) {
        return ResponseEntity.ok(addressService.getAddresses(clientId));
    }

    /**
     * Delete an address.
     * <p>{@code DELETE /api/v1/clients/{clientId}/addresses/{addressId}} â†’ 204 No Content
     */
    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAnyRole('admin', 'account_manager')")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long clientId,
            @PathVariable Long addressId,
            @AuthenticationPrincipal Jwt jwt) {

        addressService.deleteAddress(clientId, addressId, jwt);
        return ResponseEntity.noContent().build();
    }
}
