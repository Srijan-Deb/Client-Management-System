package com.cms.billing.domain.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PaymentRequest {

    @NotNull(message = "invoiceId is required")
    private Long invoiceId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
    private BigDecimal amount;

    @NotBlank(message = "recipientEmail is required")
    @Email(message = "recipientEmail must be valid")
    @Size(max = 255, message = "recipientEmail must not exceed 255 characters")
    private String recipientEmail;

    @NotBlank(message = "paymentMethodToken is required")
    @Size(max = 255, message = "paymentMethodToken must not exceed 255 characters")
    private String paymentMethodToken;

    @NotBlank(message = "idempotencyKey is required")
    @Size(max = 128, message = "idempotencyKey must not exceed 128 characters")
    private String idempotencyKey;

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethodToken() {
        return paymentMethodToken;
    }

    public void setPaymentMethodToken(String paymentMethodToken) {
        this.paymentMethodToken = paymentMethodToken;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
