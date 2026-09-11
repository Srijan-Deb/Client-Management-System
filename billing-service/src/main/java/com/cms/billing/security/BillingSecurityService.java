package com.cms.billing.security;

import com.cms.billing.domain.entity.Invoice;
import com.cms.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service("billingSecurityService")
@RequiredArgsConstructor
@Slf4j
public class BillingSecurityService {

    private final RestTemplate restTemplate;
    private final InvoiceRepository invoiceRepository;
    
    // Internal URL to client-service
    private static final String CLIENT_SERVICE_URL = "http://localhost:8081/api/v1/internal/clients/by-email?email=";

    /**
     * Verifies if the authenticated JWT (email) is the owner of the requested clientId.
     * Calls client-service internally to resolve email to clientId.
     */
    public boolean isOwner(Jwt jwt, Long targetClientId) {
        if (targetClientId == null) {
            return false;
        }
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            return false;
        }

        try {
            Long actualClientId = restTemplate.getForObject(CLIENT_SERVICE_URL + email, Long.class);
            return targetClientId.equals(actualClientId);
        } catch (Exception e) {
            log.error("Failed to verify client ownership for email: {}", email, e);
            return false;
        }
    }

    /**
     * Verifies if the authenticated JWT (email) owns the specified invoice.
     */
    public boolean isInvoiceOwner(Jwt jwt, Long invoiceId) {
        if (invoiceId == null) {
            return false;
        }
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
        if (invoiceOpt.isEmpty()) {
            return false;
        }
        return isOwner(jwt, invoiceOpt.get().getClientId());
    }

    /**
     * Resolves the clientId for the given JWT by calling client-service.
     */
    public Long resolveClientId(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            throw new IllegalArgumentException("No email found in JWT");
        }
        try {
            return restTemplate.getForObject(CLIENT_SERVICE_URL + email, Long.class);
        } catch (Exception e) {
            log.error("Failed to resolve client ID for email: {}", email, e);
            throw new IllegalArgumentException("Could not resolve client ID for email");
        }
    }
}
