package com.cms.billing.controller;

import com.cms.billing.domain.dto.PaymentRequest;
import com.cms.billing.domain.dto.PaymentResponse;
import com.cms.billing.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Process a payment for an invoice.
     * - admin / account_manager: can process payment for any invoice.
     * - client: can only pay invoices they own (ownership verified via @billingSecurityService.isInvoiceOwner).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager') or (hasRole('client') and @billingSecurityService.isInvoiceOwner(principal, #request.invoiceId))")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String performedBy = jwt != null ? jwt.getClaimAsString("preferred_username") : "system";
        if (performedBy == null && jwt != null) {
            performedBy = jwt.getSubject();
        }
        log.info("POST /billing/payments – invoiceId={} by {}", request.getInvoiceId(), performedBy);
        PaymentResponse response = paymentService.processPayment(request, performedBy);
        if ("PAID".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(400).body(response);
        }
    }
}
