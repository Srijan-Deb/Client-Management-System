package com.cms.billing.service;

import com.cms.billing.domain.dto.PaymentRequest;
import com.cms.billing.domain.dto.PaymentResponse;
import com.cms.billing.domain.entity.AuditLog;
import com.cms.billing.domain.entity.Invoice;
import com.cms.billing.domain.entity.Payment;
import com.cms.billing.repository.AuditLogRepository;
import com.cms.billing.repository.InvoiceRepository;
import com.cms.billing.repository.PaymentRepository;
import com.cms.common.event.PaymentFailedEvent;
import com.cms.common.event.PaymentSuccessEvent;
import com.stripe.model.Charge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceCacheService invoiceCacheService;
    private final AuditLogRepository auditLogRepository;
    private final StripeGateway stripeService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransactionTemplate requiresNewTxTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          InvoiceCacheService invoiceCacheService,
                          AuditLogRepository auditLogRepository,
                          StripeGateway stripeService,
                          KafkaTemplate<String, Object> kafkaTemplate,
                          PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceCacheService = invoiceCacheService;
        this.auditLogRepository = auditLogRepository;
        this.stripeService = stripeService;
        this.kafkaTemplate = kafkaTemplate;
        this.requiresNewTxTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public PaymentResponse processPayment(PaymentRequest request, String performedBy) {
        // 1. Check Idempotency upfront
        Optional<Payment> existingOpt = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            if ("PAID".equals(existing.getStatus()) || "FAILED".equals(existing.getStatus())) {
                return new PaymentResponse(existing.getId(), existing.getStatus(), existing.getStripePaymentId());
            }
        }

        // 2. Validate Invoice and record PROCESSING payment in a committed transaction
        Payment payment;
        try {
            payment = requiresNewTxTemplate.execute(status -> {
                Optional<Payment> alreadySaved = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
                if (alreadySaved.isPresent()) {
                    return alreadySaved.get();
                }

                Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + request.getInvoiceId()));

                if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
                    throw new IllegalStateException("Invoice is already marked as PAID.");
                }

                if (request.getAmount().compareTo(invoice.getTotalAmount()) != 0) {
                    throw new IllegalArgumentException("Payment amount does not match invoice total amount.");
                }

                Payment newPayment = new Payment();
                newPayment.setInvoice(invoice);
                newPayment.setAmount(request.getAmount());
                newPayment.setIdempotencyKey(request.getIdempotencyKey());
                newPayment.setStatus("PROCESSING");
                return paymentRepository.saveAndFlush(newPayment);
            });
        } catch (DataIntegrityViolationException e) {
            Payment existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElseThrow();
            return new PaymentResponse(existing.getId(), existing.getStatus(), existing.getStripePaymentId());
        }

        if (payment == null) {
            throw new IllegalStateException("Could not initialize payment transaction");
        }

        // If payment was already completed previously, return response immediately
        if ("PAID".equals(payment.getStatus())) {
            return new PaymentResponse(payment.getId(), payment.getStatus(), payment.getStripePaymentId());
        }

        final Long paymentId = payment.getId();
        final Long invoiceId = request.getInvoiceId();
        final Invoice invoiceSnapshot = invoiceRepository.findById(invoiceId).orElseThrow();
        final String currency = invoiceSnapshot.getCurrency();
        final Long clientId = invoiceSnapshot.getClientId();
        final String invoiceNumber = invoiceSnapshot.getInvoiceNumber();

        String recipientEmail = request.getRecipientEmail();
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            recipientEmail = "client@cms.com";
        }
        final String effectiveRecipientEmail = recipientEmail;

        try {
            // 3. Call Stripe (external network call outside any DB transaction)
            Charge charge = stripeService.createCharge(
                    request.getPaymentMethodToken(),
                    request.getAmount(),
                    currency,
                    "Payment for Invoice: " + invoiceNumber
            );

            // 4. Update Payment to PAID and mark Invoice as PAID in a committed transaction
            Payment paidPayment = requiresNewTxTemplate.execute(status -> {
                Payment p = paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new IllegalStateException("Payment not found with id: " + paymentId));
                p.setStatus("PAID");
                p.setStripePaymentId(charge.getId());
                Payment saved = paymentRepository.saveAndFlush(p);

                Invoice inv = invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new IllegalStateException("Invoice not found with id: " + invoiceId));
                inv.setStatus("PAID");
                invoiceRepository.saveAndFlush(inv);

                AuditLog auditLog = new AuditLog();
                auditLog.setEntityName("Payment");
                auditLog.setEntityId(saved.getId());
                auditLog.setAction("CREATE");
                auditLog.setPerformedBy(performedBy != null ? performedBy : "system");
                auditLog.setDetails("Payment successful via Stripe: " + charge.getId());
                auditLogRepository.save(auditLog);

                return saved;
            });

            // Evict/update invoice in cache
            try {
                invoiceCacheService.updateInvoice(invoiceRepository.findById(invoiceId).orElse(invoiceSnapshot));
            } catch (Exception ex) {
                log.warn("Failed to evict invoice cache: {}", ex.getMessage());
            }

            // 5. Publish PaymentSuccessEvent to Kafka
            PaymentSuccessEvent successEvent = PaymentSuccessEvent.builder()
                    .paymentId(paidPayment.getId())
                    .invoiceId(invoiceId)
                    .clientId(clientId)
                    .recipientEmail(effectiveRecipientEmail)
                    .amountPaid(paidPayment.getAmount())
                    .currency(currency)
                    .transactionRef(charge.getId())
                    .gateway("STRIPE")
                    .paymentMethod("CARD")
                    .paidAt(Instant.now())
                    .build();
            kafkaTemplate.send(PaymentSuccessEvent.TOPIC, successEvent);

            return new PaymentResponse(paidPayment.getId(), paidPayment.getStatus(), paidPayment.getStripePaymentId());

        } catch (Exception e) {
            log.error("Payment failed for invoice {}: {}", invoiceId, e.getMessage(), e);

            Payment failedPayment = requiresNewTxTemplate.execute(status -> {
                Payment p = paymentRepository.findById(paymentId).orElse(null);
                if (p != null) {
                    p.setStatus("FAILED");
                    return paymentRepository.saveAndFlush(p);
                }
                return null;
            });

            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .paymentId(paymentId)
                    .invoiceId(invoiceId)
                    .clientId(clientId)
                    .recipientEmail(effectiveRecipientEmail)
                    .failureReason(e.getMessage())
                    .gatewayErrorCode("STRIPE_ERROR")
                    .gateway("STRIPE")
                    .paymentMethod("CARD")
                    .failedAt(Instant.now())
                    .build();
            kafkaTemplate.send(PaymentFailedEvent.TOPIC, failedEvent);

            return new PaymentResponse(paymentId, failedPayment != null ? failedPayment.getStatus() : "FAILED", null);
        }
    }
}
