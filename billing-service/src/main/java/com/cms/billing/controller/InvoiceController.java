package com.cms.billing.controller;

import com.cms.billing.domain.entity.Invoice;
import com.cms.billing.repository.InvoiceRepository;
import com.cms.billing.security.BillingSecurityService;
import com.cms.billing.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final BillingSecurityService billingSecurityService;
    private final MinioService minioService;

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
     * List all invoices, optionally filtered by clientId.
     * - admin / account_manager / support_agent: can list any client's invoices.
     * - client: clientId param is ignored; always resolves to their own clientId.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent') or hasRole('client')")
    public ResponseEntity<List<Invoice>> listInvoices(
            @RequestParam(required = false) Long clientId,
            @AuthenticationPrincipal Jwt jwt) {

        if (isClientOnly(jwt)) {
            clientId = billingSecurityService.resolveClientId(jwt);
        }

        log.info("GET /billing/invoices?clientId={}", clientId);
        List<Invoice> invoices;
        if (clientId != null) {
            invoices = invoiceRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        } else {
            invoices = invoiceRepository.findAll();
        }
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get a presigned download URL for an invoice PDF stored in MinIO.
     */
    @GetMapping("/pdf-url")
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent') or hasRole('client')")
    public ResponseEntity<Map<String, String>> getPdfUrl(@RequestParam String objectKey) {
        log.info("GET /billing/invoices/pdf-url?objectKey={}", objectKey);
        try {
            String url = minioService.getPresignedUrl(objectKey);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for objectKey: {}", objectKey, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
