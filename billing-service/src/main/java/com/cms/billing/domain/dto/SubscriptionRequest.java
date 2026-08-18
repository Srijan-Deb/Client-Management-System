package com.cms.billing.domain.dto;

import jakarta.validation.constraints.NotNull;

public class SubscriptionRequest {
    @NotNull(message = "clientId is required")
    private Long clientId;
    
    @NotNull(message = "productId is required")
    private Long productId;

    @jakarta.validation.constraints.NotBlank(message = "recipientEmail is required")
    @jakarta.validation.constraints.Email(message = "recipientEmail must be valid")
    private String recipientEmail;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
}
