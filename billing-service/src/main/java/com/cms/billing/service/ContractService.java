package com.cms.billing.service;

import com.cms.billing.domain.dto.ContractRequest;
import com.cms.billing.domain.dto.ContractResponse;
import com.cms.billing.domain.entity.AuditLog;
import com.cms.billing.domain.entity.Contract;
import com.cms.billing.domain.entity.Invoice;
import com.cms.billing.domain.entity.Product;
import com.cms.billing.domain.entity.Subscription;
import com.cms.billing.repository.AuditLogRepository;
import com.cms.billing.repository.ContractRepository;
import com.cms.billing.repository.InvoiceRepository;
import com.cms.billing.repository.ProductRepository;
import com.cms.common.event.InvoiceGeneratedEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractService {

    private final ProductRepository productRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final MinioService minioService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ContractService(ProductRepository productRepository,
                           ContractRepository contractRepository,
                           InvoiceRepository invoiceRepository,
                           AuditLogRepository auditLogRepository,
                           PdfGeneratorService pdfGeneratorService,
                           MinioService minioService,
                           KafkaTemplate<String, Object> kafkaTemplate) {
        this.productRepository = productRepository;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditLogRepository = auditLogRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.minioService = minioService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public ContractResponse createContract(ContractRequest request, String performedBy) {
        List<Product> products = productRepository.findAllById(request.getProductIds());
        if (products.isEmpty()) {
            throw new IllegalArgumentException("No valid products found for contract");
        }

        BigDecimal subtotal = products.stream()
                .map(Product::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tax logic (assume 18%)
        BigDecimal taxRate = new BigDecimal("18.00");
        BigDecimal taxAmount = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        // Create Contract
        Contract contract = new Contract();
        contract.setClientId(request.getClientId());
        contract.setAccountId(request.getAccountId());
        contract.setStatus("ACTIVE");
        contract.setTotalValue(totalAmount);
        contract.setStartDate(LocalDate.now());
        // set end_date logic if needed based on products

        // Create Subscriptions
        for (Product product : products) {
            Subscription sub = new Subscription();
            sub.setClientId(request.getClientId());
            sub.setProduct(product);
            sub.setStatus("ACTIVE");
            sub.setStartDate(LocalDate.now());
            
            LocalDate nextBilling = LocalDate.now();
            if ("MONTHLY".equalsIgnoreCase(product.getBillingCycle())) {
                nextBilling = nextBilling.plusMonths(1);
            } else if ("YEARLY".equalsIgnoreCase(product.getBillingCycle())) {
                nextBilling = nextBilling.plusYears(1);
            }
            sub.setNextBillingDate(nextBilling);
            contract.addSubscription(sub);
        }

        // Create Invoice
        Invoice invoice = new Invoice();
        invoice.setClientId(request.getClientId());
        invoice.setAccountId(request.getAccountId());
        invoice.setInvoiceNumber("INV-" + LocalDate.now().getYear() + "-" + System.currentTimeMillis());
        invoice.setSubtotal(subtotal);
        invoice.setTaxRate(taxRate);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setCurrency("USD");
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setStatus("PENDING");
        contract.addInvoice(invoice);

        // Save everything
        Contract savedContract = contractRepository.save(contract);
        Invoice savedInvoice = savedContract.getInvoices().get(0);

        // Audit Log
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName("Contract");
        auditLog.setEntityId(savedContract.getId());
        auditLog.setAction("CREATE");
        auditLog.setPerformedBy(performedBy != null ? performedBy : "system");
        auditLog.setDetails("Created contract with " + products.size() + " products.");
        auditLogRepository.save(auditLog);

        // Generate PDF & Upload
        byte[] pdfBytes = pdfGeneratorService.generateContractInvoicePdf(savedContract, savedInvoice);
        String objectKey = "invoices/" + savedContract.getClientId() + "/" + savedInvoice.getInvoiceNumber() + ".pdf";
        try {
            minioService.uploadPdf(objectKey, pdfBytes);
            savedContract.setPdfUrl(objectKey);
            savedInvoice.setPdfObjectKey(objectKey);
            // JPA will cascade update because they are managed entities in this transaction
        } catch (Exception e) {
            // log error, potentially fail transaction or handle gracefully
            throw new RuntimeException("Failed to upload PDF", e);
        }

        // Publish Event
        InvoiceGeneratedEvent event = InvoiceGeneratedEvent.builder()
                .invoiceId(savedInvoice.getId())
                .contractId(savedContract.getId())
                .accountId(savedInvoice.getAccountId())
                .clientId(savedInvoice.getClientId())
                .recipientEmail(request.getRecipientEmail())
                .invoiceNumber(savedInvoice.getInvoiceNumber())
                .subtotal(savedInvoice.getSubtotal())
                .taxRate(savedInvoice.getTaxRate())
                .taxAmount(savedInvoice.getTaxAmount())
                .totalAmount(savedInvoice.getTotalAmount())
                .currency(savedInvoice.getCurrency())
                .dueDate(savedInvoice.getDueDate())
                .pdfObjectKey(objectKey)
                .generatedAt(Instant.now())
                .build();
        kafkaTemplate.send(InvoiceGeneratedEvent.TOPIC, event);

        // Build Response
        ContractResponse response = new ContractResponse();
        response.setContractId(savedContract.getId());
        response.setInvoiceId(savedInvoice.getId());
        response.setInvoiceNumber(savedInvoice.getInvoiceNumber());
        response.setTotalValue(savedContract.getTotalValue());
        response.setPdfUrl(savedContract.getPdfUrl());
        response.setStartDate(savedContract.getStartDate());

        return response;
    }
}
