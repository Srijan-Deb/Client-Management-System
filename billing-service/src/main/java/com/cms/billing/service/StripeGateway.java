package com.cms.billing.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import java.math.BigDecimal;

public interface StripeGateway {
    Charge createCharge(String token, BigDecimal amount, String currency, String description) throws StripeException;
}
