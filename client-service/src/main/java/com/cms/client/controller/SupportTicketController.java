package com.cms.client.controller;

import com.cms.client.dto.request.TicketCommentRequest;
import com.cms.client.dto.request.TicketRequest;
import com.cms.client.dto.response.TicketResponse;
import com.cms.client.security.ClientSecurityService;
import com.cms.client.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class SupportTicketController {

    private final SupportTicketService ticketService;
    private final ClientSecurityService clientSecurityService;

    /** Determines if the JWT only carries the 'client' role (not admin/agent). */
    private boolean isClientOnly(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?>)) return false;
        List<?> roles = (List<?>) rolesObj;
        return roles.contains("client")
                && !roles.contains("admin")
                && !roles.contains("account_manager")
                && !roles.contains("support_agent");
    }

    /**
     * Create a ticket.
     * - admin / account_manager / support_agent: can create for any clientId.
     * - client: can only create for their own clientId (enforced via SpEL ownership check).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent') or (hasRole('client') and @clientSecurityService.isOwner(principal, #request.clientId))")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /tickets – subject={}", request.getSubject());
        TicketResponse response = ticketService.createTicket(request, jwt);
        URI location = URI.create("/api/v1/tickets/" + response.getTicketId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Get a single ticket by ID.
     * - admin / account_manager / support_agent: can view any ticket.
     * - client: can only view tickets they own.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent') or (hasRole('client') and @clientSecurityService.isTicketOwner(principal, #id))")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        log.info("GET /tickets/{}", id);
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    /**
     * List tickets, optionally by clientId.
     * - admin / account_manager / support_agent: can list any client's tickets.
     * - client: clientId param is ignored; always resolves to their own clientId.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent') or hasRole('client')")
    public ResponseEntity<Page<TicketResponse>> getTicketsByClient(
            @RequestParam(required = false) Long clientId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {

        if (isClientOnly(jwt)) {
            clientId = clientSecurityService.resolveClientId(jwt);
        }

        log.info("GET /tickets?clientId={}", clientId);
        return ResponseEntity.ok(ticketService.getTicketsByClient(clientId, pageable));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('admin', 'support_agent')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long id,
            @RequestParam Long agentId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /tickets/{}/assign – agentId={}", id, agentId);
        return ResponseEntity.ok(ticketService.assignTicket(id, agentId, jwt));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('admin', 'support_agent')")
    public ResponseEntity<TicketResponse> resolveTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /tickets/{}/resolve", id);
        return ResponseEntity.ok(ticketService.resolveTicket(id, jwt));
    }

    /**
     * Reopen a ticket.
     * - admin / support_agent: can reopen any ticket.
     * - client: can only reopen their own tickets.
     */
    @PutMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('admin', 'support_agent') or (hasRole('client') and @clientSecurityService.isTicketOwner(principal, #id))")
    public ResponseEntity<TicketResponse> reopenTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /tickets/{}/reopen", id);
        return ResponseEntity.ok(ticketService.reopenTicket(id, jwt));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('admin', 'support_agent')")
    public ResponseEntity<TicketResponse> closeTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /tickets/{}/close", id);
        return ResponseEntity.ok(ticketService.closeTicket(id, jwt));
    }

    /**
     * Add a comment to a ticket.
     * - admin / support_agent / account_manager: can comment on any ticket.
     * - client: can only comment on their own tickets.
     */
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('admin', 'support_agent', 'account_manager') or (hasRole('client') and @clientSecurityService.isTicketOwner(principal, #id))")
    public ResponseEntity<TicketResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody TicketCommentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /tickets/{}/comments", id);
        return ResponseEntity.ok(ticketService.addComment(id, request, jwt));
    }
}
