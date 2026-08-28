package com.cms.billing.controller;

import com.cms.billing.repository.InvoiceRepository;
import com.cms.billing.repository.SubscriptionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class BillingDashboardController {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;

    public BillingDashboardController(SubscriptionRepository subscriptionRepository, InvoiceRepository invoiceRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/metrics")
    public Map<String, Long> getMetrics() {
        long activeSubscriptions = subscriptionRepository.countByStatus("ACTIVE");
        long outstandingInvoices = invoiceRepository.countByStatus("PENDING"); // Using PENDING as requested
        
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("activeSubscriptions", activeSubscriptions);
        metrics.put("outstandingInvoices", outstandingInvoices);
        return metrics;
    }
}
