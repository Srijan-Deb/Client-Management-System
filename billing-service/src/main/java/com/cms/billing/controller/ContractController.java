package com.cms.billing.controller;

import com.cms.billing.domain.dto.ContractRequest;
import com.cms.billing.domain.dto.ContractResponse;
import com.cms.billing.domain.entity.Contract;
import com.cms.billing.repository.ContractRepository;
import com.cms.billing.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/contracts")
@RequiredArgsConstructor
@Slf4j
public class ContractController {

    private final ContractService contractService;
    private final ContractRepository contractRepository;

    /**
     * Create a contract for a client.
     * This is the primary invoice-generation trigger:
     * it creates the contract, activates subscriptions, generates a PDF invoice,
     * uploads it to MinIO, and publishes an InvoiceGeneratedEvent to Kafka
     * which the notification-service consumes to email the invoice to the client.
     *
     * Access: admin, account_manager only.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin','account_manager')")
    public ResponseEntity<ContractResponse> createContract(
            @Valid @RequestBody ContractRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String performedBy = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        if (performedBy == null && jwt != null) {
            performedBy = jwt.getSubject();
        }
        log.info("POST /billing/contracts – clientId={} by {}", request.getClientId(), performedBy);
        ContractResponse response = contractService.createContract(request, performedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all contracts for a given client, newest first.
     * Access: admin, account_manager (full visibility); support_agent (read-only context).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin','account_manager','support_agent')")
    public ResponseEntity<List<Contract>> listContracts(
            @RequestParam(required = false) Long clientId) {
        List<Contract> contracts;
        if (clientId != null) {
            log.info("GET /billing/contracts?clientId={}", clientId);
            contracts = contractRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        } else {
            log.info("GET /billing/contracts (all)");
            contracts = contractRepository.findAll();
        }
        return ResponseEntity.ok(contracts);
    }
}
