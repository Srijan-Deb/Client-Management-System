package com.cms.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka event published to topic <b>payment-failed</b> by Billing Service
 * when a payment gateway callback indicates failure or when the idempotent
 * retry logic exhausts all attempts.
 *
 * <p>All ID fields are {@code Long} matching BIGINT AUTO_INCREMENT PKs in MySQL.
 *
 * <p>The retry decision is made <em>before</em> this event is published.
 * See the PAYMENTFLOW flowchart for the Retry? branch logic.
 *
 * <p>Consumed by:
 * <ul>
 *   <li>Notification Service â€” sends payment failure alert email</li>
 *   <li>Billing Service (self-consumed) â€” marks invoice with PAYMENT_FAILED status</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {

    /** Topic name constant. */
    public static final String TOPIC = "payment-failed";

    /** {@code payments.payment_id} â€” BIGINT PK of the failed attempt. */
    @NotNull
    private Long paymentId;

    /** {@code payments.invoice_id} â€” FK to invoices. */
    @NotNull
    private Long invoiceId;

    /** Denormalised client ID for notification routing. */
    @NotNull
    private Long clientId;

    /**
     * Denormalised recipient email address â€” client's primary email at failure time.
     * Carried on the event so Notification Service can route the alert email
     * without making a synchronous call to Client Service.
     */
    @NotBlank
    private String recipientEmail;

    /**
     * Human-readable failure reason from the gateway
     * (e.g. "INSUFFICIENT_FUNDS", "CARD_DECLINED", "NETWORK_ERROR").
     */
    @NotBlank
    private String failureReason;

    /**
     * Gateway-specific error code (for support/debugging).
     * Matches {@code payments.gateway} context.
     */
    private String gatewayErrorCode;

    /** Gateway name (e.g. RAZORPAY, STRIPE). */
    private String gateway;

    /**
     * Payment method that was attempted. Matches {@code payments.payment_method}
     * in CMS_Schema_Merged.sql (e.g. CARD, UPI, BANK_TRANSFER, WALLET).
     *
     * <p>Carried here so Notification Service can render
     * "Your CARD payment failed" without an extra DB lookup.
     */
    private String paymentMethod;

    /** Number of retry attempts that were made before giving up. */
    private int retryAttempts;

    /** Whether further automatic retries will be attempted. */
    private boolean retryExhausted;

    /** UTC timestamp of the final failure. */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant failedAt;
}
