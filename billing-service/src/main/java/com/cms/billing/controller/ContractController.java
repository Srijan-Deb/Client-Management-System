package com.cms.billing.controller;

import com.cms.billing.domain.dto.ContractRequest;
import com.cms.billing.domain.dto.ContractResponse;
import com.cms.billing.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('admin','account_manager')")
    public ResponseEntity<ContractResponse> createContract(@Valid @RequestBody ContractRequest request,
                                                           @AuthenticationPrincipal Jwt jwt) {
        String performedBy = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        if (performedBy == null) {
            performedBy = jwt.getSubject();
        }
        ContractResponse response = contractService.createContract(request, performedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
