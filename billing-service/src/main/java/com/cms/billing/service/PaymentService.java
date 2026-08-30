package com.cms.billing.service;

import com.cms.billing.domain.dto.PaymentRequest;
import com.cms.billing.domain.dto.PaymentResponse;
import com.cms.billing.domain.entity.AuditLog;
import com.cms.billing.domain.entity.Invoice;
import com.cms.billing.domain.entity.Payment;
import com.cms.billing.repository.AuditLogRepository;
import com.cms.billing.repository.PaymentRepository;
import com.cms.common.event.PaymentFailedEvent;
import com.cms.common.event.PaymentSuccessEvent;
import com.stripe.model.Charge;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceCacheService invoiceCacheService;
    private final AuditLogRepository auditLogRepository;
    private final StripeGateway stripeService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransactionTemplate requiresNewTxTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          InvoiceCacheService invoiceCacheService,
                          AuditLogRepository auditLogRepository,
                          StripeGateway stripeService,
                          KafkaTemplate<String, Object> kafkaTemplate,
                          PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.invoiceCacheService = invoiceCacheService;
        this.auditLogRepository = auditLogRepository;
        this.stripeService = stripeService;
        this.kafkaTemplate = kafkaTemplate;
        this.requiresNewTxTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String performedBy) {
        // 1. Check Idempotency
        Optional<Payment> existingPaymentOpt = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingPaymentOpt.isPresent()) {
            Payment existing = existingPaymentOpt.get();
            return new PaymentResponse(existing.getId(), existing.getStatus(), existing.getStripePaymentId());
        }

        // 2. Fetch Invoice (Cache -> Fallback to MySQL)
        Invoice invoice = invoiceCacheService.getInvoiceById(request.getInvoiceId());

        // 3. Amount Validation
        if (request.getAmount().compareTo(invoice.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match invoice total amount.");
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(request.getAmount());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setStatus("PROCESSING");
        
        final Payment finalPayment = payment;
        try {
            payment = requiresNewTxTemplate.execute(status -> paymentRepository.saveAndFlush(finalPayment));
        } catch (DataIntegrityViolationException e) {
            Payment existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElseThrow();
            return new PaymentResponse(existing.getId(), existing.getStatus(), existing.getStripePaymentId());
        }

        try {
            // 4. Call Stripe
            Charge charge = stripeService.createCharge(
                    request.getPaymentMethodToken(),
                    request.getAmount(),
                    invoice.getCurrency(),
                    "Payment for Invoice: " + invoice.getInvoiceNumber()
            );

            // 5. Success Flow
            payment.setStatus("PAID");
            payment.setStripePaymentId(charge.getId());
            Payment savedPayment = paymentRepository.save(payment);

            invoice.setStatus("PAID");
            invoiceCacheService.updateInvoice(invoice);

            AuditLog auditLog = new AuditLog();
            auditLog.setEntityName("Payment");
            auditLog.setEntityId(savedPayment.getId());
            auditLog.setAction("CREATE");
            auditLog.setPerformedBy(performedBy != null ? performedBy : "system");
            auditLog.setDetails("Payment successful via Stripe: " + charge.getId());
            auditLogRepository.save(auditLog);

            PaymentSuccessEvent successEvent = PaymentSuccessEvent.builder()
                    .paymentId(savedPayment.getId())
                    .invoiceId(invoice.getId())
                    .clientId(invoice.getClientId())
                    .recipientEmail(request.getRecipientEmail())
                    .amountPaid(savedPayment.getAmount())
                    .currency(invoice.getCurrency())
                    .transactionRef(charge.getId())
                    .gateway("STRIPE")
                    .paymentMethod("CARD")
                    .paidAt(Instant.now())
                    .build();
            kafkaTemplate.send(PaymentSuccessEvent.TOPIC, successEvent);

            return new PaymentResponse(savedPayment.getId(), savedPayment.getStatus(), savedPayment.getStripePaymentId());

        } catch (Exception e) {
            // 6. Failure Flow
            payment.setStatus("FAILED");
            Payment savedPayment = paymentRepository.save(payment);

            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .paymentId(savedPayment.getId())
                    .invoiceId(invoice.getId())
                    .clientId(invoice.getClientId())
                    .recipientEmail(request.getRecipientEmail())
                    .failureReason(e.getMessage())
                    .gatewayErrorCode("STRIPE_ERROR")
                    .gateway("STRIPE")
                    .paymentMethod("CARD")
                    .failedAt(Instant.now())
                    .build();
            kafkaTemplate.send(PaymentFailedEvent.TOPIC, failedEvent);

            return new PaymentResponse(savedPayment.getId(), savedPayment.getStatus(), null);
        }
    }
}
