package com.cms.client.controller;

import com.cms.client.dto.request.ContactRequest;
import com.cms.client.dto.response.ContactResponse;
import com.cms.client.service.ContactService;
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
 * REST controller for the {@code contacts} sub-resource.
 *
 * <p>Base path: {@code /api/v1/clients/{clientId}/contacts}
 *
 * <p>Contacts have an independent lifecycle from the parent client:
 * the client must exist before contacts can be added.
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /**
     * Add a contact to an existing client.
     * <p>{@code POST /api/v1/clients/{clientId}/contacts} â†’ 201 Created
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager')")
    public ResponseEntity<ContactResponse> addContact(
            @PathVariable Long clientId,
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        ContactResponse response = contactService.addContact(clientId, request, jwt);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{contactId}")
                .buildAndExpand(response.getContactId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * List all contacts for a client.
     * <p>{@code GET /api/v1/clients/{clientId}/contacts} â†’ 200 OK
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent')")
    public ResponseEntity<List<ContactResponse>> getContacts(@PathVariable Long clientId) {
        return ResponseEntity.ok(contactService.getContacts(clientId));
    }

    /**
     * Delete a contact.
     * <p>{@code DELETE /api/v1/clients/{clientId}/contacts/{contactId}} â†’ 204 No Content
     */
    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('admin', 'account_manager')")
    public ResponseEntity<Void> deleteContact(
            @PathVariable Long clientId,
            @PathVariable Long contactId,
            @AuthenticationPrincipal Jwt jwt) {

        contactService.deleteContact(clientId, contactId, jwt);
        return ResponseEntity.noContent().build();
    }
}
