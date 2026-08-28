package com.cms.client.controller;

import com.cms.client.dto.request.TicketCommentRequest;
import com.cms.client.dto.request.TicketRequest;
import com.cms.client.dto.response.TicketResponse;
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

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class SupportTicketController {

    private final SupportTicketService ticketService;

    @PostMapping
    @PreAuthorize("hasAnyRole('client', 'admin', 'account_manager', 'support_agent')")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /tickets â€” subject={}", request.getSubject());
        TicketResponse response = ticketService.createTicket(request, jwt);
        URI location = URI.create("/api/v1/tickets/" + response.getTicketId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('client', 'admin', 'account_manager', 'support_agent')")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        log.info("GET /tickets/{}", id);
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('client', 'admin', 'account_manager', 'support_agent')")
    public ResponseEntity<Page<TicketResponse>> getTicketsByClient(
            @RequestParam(required = false) Long clientId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("GET /tickets?clientId={}", clientId);
        return ResponseEntity.ok(ticketService.getTicketsByClient(clientId, pageable));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('admin', 'support_agent')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long id,
            @RequestParam Long agentId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /tickets/{}/assign â€” agentId={}", id, agentId);
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

    @PutMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('client', 'admin', 'support_agent')")
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

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('client', 'admin', 'support_agent', 'account_manager')")
    public ResponseEntity<TicketResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody TicketCommentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /tickets/{}/comments", id);
        return ResponseEntity.ok(ticketService.addComment(id, request, jwt));
    }
}
