package com.cms.client.controller;

import com.cms.client.domain.entity.Client;
import com.cms.client.repository.ClientRepository;
import com.cms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/clients")
@RequiredArgsConstructor
@Slf4j
public class InternalClientController {

    private final ClientRepository clientRepository;

    /**
     * Internal endpoint to resolve a client ID by email.
     * Accessible by other microservices (e.g. billing-service) to enforce data-level security.
     */
    @GetMapping("/by-email")
    public ResponseEntity<Long> getClientIdByEmail(@RequestParam String email) {
        log.info("Internal call to resolve client ID for email: {}", email);
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND", "Client not found for email"));
        return ResponseEntity.ok(client.getClientId());
    }
}
