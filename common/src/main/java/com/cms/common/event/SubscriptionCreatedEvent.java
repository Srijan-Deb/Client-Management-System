package com.cms.common.event;

public class SubscriptionCreatedEvent {

    private Long subscriptionId;
    private Long clientId;
    private Long productId;
    private String recipientEmail;

    public SubscriptionCreatedEvent() {
    }

    public SubscriptionCreatedEvent(Long subscriptionId, Long clientId, Long productId, String recipientEmail) {
        this.subscriptionId = subscriptionId;
        this.clientId = clientId;
        this.productId = productId;
        this.recipientEmail = recipientEmail;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

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
