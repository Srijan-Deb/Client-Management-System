package com.cms.billing.controller;

import com.cms.billing.domain.entity.Invoice;
import com.cms.billing.repository.InvoiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    public InvoiceController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * List all invoices, optionally filtered by clientId.
     * Accessible by admin, account_manager, and support_agent.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent')")
    public ResponseEntity<List<Invoice>> listInvoices(
            @RequestParam(required = false) Long clientId) {
        List<Invoice> invoices;
        if (clientId != null) {
            invoices = invoiceRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        } else {
            invoices = invoiceRepository.findAll();
        }
        return ResponseEntity.ok(invoices);
    }
}
