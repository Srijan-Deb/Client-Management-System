package com.cms.billing.domain.dto;

public class PaymentResponse {
    private Long paymentId;
    private String status;
    private String stripePaymentId;

    public PaymentResponse(Long paymentId, String status, String stripePaymentId) {
        this.paymentId = paymentId;
        this.status = status;
        this.stripePaymentId = stripePaymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getStatus() {
        return status;
    }

    public String getStripePaymentId() {
        return stripePaymentId;
    }
}
