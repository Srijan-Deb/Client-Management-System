package com.cms.billing.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService implements StripeGateway {

    public StripeService(@Value("${stripe.api-key}") String apiKey) {
        Stripe.apiKey = apiKey;
    }

    public Charge createCharge(String token, BigDecimal amount, String currency, String description) throws StripeException {
        // Stripe expects amount in cents
        long amountInCents = amount.multiply(new BigDecimal(100)).longValue();

        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", amountInCents);
        chargeParams.put("currency", currency);
        chargeParams.put("description", description);
        chargeParams.put("source", token);

        return Charge.create(chargeParams);
    }
}
