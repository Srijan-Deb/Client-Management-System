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
import java.time.LocalDate;

/**
 * Kafka event published to topic <b>invoice-generated</b> by Billing Service
 * whenever a new invoice is created (from contract or manual trigger).
 *
 * <p>All ID fields are {@code Long} matching BIGINT AUTO_INCREMENT PKs in MySQL.
 *
 * <p>Consumed by:
 * <ul>
 *   <li>Notification Service Ã¢â‚¬â€ sends invoice email with PDF link</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceGeneratedEvent {

    /** Topic name constant. */
    public static final String TOPIC = "invoice-generated";

    /** {@code invoices.invoice_id} Ã¢â‚¬â€ BIGINT PK. */
    @NotNull
    private Long invoiceId;

    /** {@code invoices.contract_id} Ã¢â‚¬â€ FK to contracts. */
    @NotNull
    private Long contractId;

    /** {@code invoices.account_id} Ã¢â‚¬â€ FK to accounts. */
    @NotNull
    private Long accountId;

    /**
     * Denormalised client ID Ã¢â‚¬â€ not a direct FK on invoices but needed
     * by Notification Service to address the email without a DB lookup.
     */
    @NotNull
    private Long clientId;

    /**
     * Denormalised recipient email address Ã¢â‚¬â€ client's primary email at invoice time.
     * Carried on the event so Notification Service can route the email without
     * making a synchronous call to Client Service.
     */
    @NotBlank
    private String recipientEmail;

    /** Human-readable invoice number (e.g. INV-2024-00042). */
    @NotBlank
    private String invoiceNumber;

    /**
     * Pre-tax line-item subtotal.
     * Matches {@code invoices.subtotal} in CMS_Schema_Merged.sql.
     */
    @NotNull
    @Positive
    private BigDecimal subtotal;

    /**
     * Applicable tax rate as a percentage (e.g. 18.00 for 18% GST).
     * Matches {@code invoices.tax_rate}.
     *
     * <p>Stored separately from {@link #taxAmount} so that consumers can
     * audit the exact rate that was in effect at invoice time, even if
     * the statutory rate changes retroactively. This was the deciding
     * factor when keeping CMS_Schema_Merged.sql over the collapsed schema.
     */
    @NotNull
    private BigDecimal taxRate;

    /**
     * Absolute tax amount charged (= subtotal Ãƒ- taxRate / 100).
     * Matches {@code invoices.tax_amount}.
     *
     * <p>Carrying both {@link #taxRate} and {@code taxAmount} lets downstream
     * services display a receipt without recomputing, while preserving the
     * rate for audit trails.
     */
    @NotNull
    private BigDecimal taxAmount;

    /**
     * Final amount due (subtotal + taxAmount).
     * Matches {@code invoices.total_amount}.
     */
    @NotNull
    @Positive
    private BigDecimal totalAmount;

    /** Currency ISO code (e.g. INR). */
    @NotBlank
    private String currency;

    /** Due date for payment. Serialised as {@code yyyy-MM-dd}. */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    /** MinIO object key for the generated PDF (null until PDF is uploaded). */
    private String pdfObjectKey;

    /** UTC timestamp when the invoice was generated. */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant generatedAt;
}
