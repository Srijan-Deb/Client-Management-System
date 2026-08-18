package com.cms.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kafka event published to topic <b>payment-success</b> by Billing Service
 * when a payment gateway callback confirms a successful payment.
 *
 * <p>All ID fields are {@code Long} matching BIGINT AUTO_INCREMENT PKs in MySQL.
 *
 * <p>Consumed by:
 * <ul>
 *   <li>Notification Service â€” sends payment receipt email</li>
 *   <li>Billing Service (self-consumed, idempotent) â€” updates invoice status to PAID</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {

    /** Topic name constant. */
    public static final String TOPIC = "payment-success";

    /** {@code payments.payment_id} â€” BIGINT PK. */
    @NotNull
    private Long paymentId;

    /** {@code payments.invoice_id} â€” FK to invoices. */
    @NotNull
    private Long invoiceId;

    /**
     * Denormalised client ID â€” needed by Notification Service
     * to address the receipt email without a DB lookup.
     */
    @NotNull
    private Long clientId;

    /**
     * Denormalised recipient email address â€” client's primary email at payment time.
     * Carried on the event so Notification Service can route the receipt email
     * without making a synchronous call to Client Service.
     */
    @NotBlank
    private String recipientEmail;

    /** Actual amount collected. Matches {@code payments.amount}. */
    @NotNull
    @Positive
    private BigDecimal amountPaid;

    /** Currency ISO code. */
    @NotBlank
    private String currency;

    /**
     * Payment gateway transaction reference.
     * Matches {@code payments.transaction_ref}.
     */
    private String transactionRef;

    /**
     * Gateway name. Matches {@code payments.gateway}
     * (e.g. RAZORPAY, STRIPE, MANUAL).
     */
    private String gateway;

    /**
     * Payment method used. Matches {@code payments.payment_method} in CMS_Schema_Merged.sql
     * (e.g. CARD, UPI, BANK_TRANSFER, WALLET).
     */
    private String paymentMethod;

    /** UTC timestamp of successful payment confirmation. */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant paidAt;
}
