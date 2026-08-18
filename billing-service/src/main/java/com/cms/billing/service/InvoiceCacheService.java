package com.cms.billing.service;

import com.cms.billing.domain.entity.Invoice;
import com.cms.billing.repository.InvoiceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class InvoiceCacheService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceCacheService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Cacheable(value = "invoices", key = "#p0")
    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
    }

    @CacheEvict(value = "invoices", key = "#p0.id")
    public Invoice updateInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }
}
