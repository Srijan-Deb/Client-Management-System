package com.cms.billing.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ContractRequest {
    @NotNull(message = "clientId is required")
    private Long clientId;
    
    @NotNull(message = "accountId is required")
    private Long accountId;
    
    @NotBlank(message = "recipientEmail is required")
    @Email(message = "recipientEmail must be valid")
    private String recipientEmail;
    
    @NotEmpty(message = "productIds cannot be empty")
    private List<Long> productIds;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
    }
}
