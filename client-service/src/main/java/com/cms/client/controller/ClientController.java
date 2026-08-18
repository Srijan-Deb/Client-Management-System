package com.cms.client.controller;

import com.cms.client.dto.request.CreateClientRequest;
import com.cms.client.dto.request.UpdateClientRequest;
import com.cms.client.dto.response.ClientResponse;
import com.cms.client.dto.response.ClientSummaryResponse;
import com.cms.client.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller for client management operations.
 *
 * <p>All endpoints require a valid JWT (enforced by the SecurityConfig resource-server
 * filter chain). RBAC is enforced at method level via {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final ClientService clientService;

    /**
     * Create a new client.
     * <p>Atomically: inserts client â†’ provisions account â†’ caches â†’ publishes Kafka event.
     *
     * @return 201 Created with {@code Location} header pointing to the new resource
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager')")
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody CreateClientRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("POST /clients â€” email={}", request.getEmail());
        ClientResponse response = clientService.createClient(request, jwt);
        URI location = URI.create("/api/v1/clients/" + response.getClientId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Get a single client by ID â€” Redis cache-aside, falls back to MySQL.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent')")
    public ResponseEntity<ClientResponse> getClient(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    /**
     * Paginated search across first name, last name, and email.
     * Omitting {@code search} returns all clients ordered by {@code created_at DESC}.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent')")
    public ResponseEntity<Page<ClientSummaryResponse>> searchClients(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(clientService.searchClients(search, pageable));
    }

    /**
     * Partial update of client details.
     * Invalidates and repopulates the Redis cache entry on success.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'account_manager')")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClientRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("PUT /clients/{} by subject={}", id, jwt.getSubject());
        return ResponseEntity.ok(clientService.updateClient(id, request, jwt));
    }
}
