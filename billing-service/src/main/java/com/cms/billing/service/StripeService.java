package com.cms.billing.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class StripeService implements StripeGateway {

    private final String apiKey;

    public StripeService(@Value("${stripe.api-key}") String apiKey) {
        this.apiKey = apiKey;
        Stripe.apiKey = apiKey;
    }

    @Override
    public Charge createCharge(String token, BigDecimal amount, String currency, String description) throws StripeException {
        // Mock simulation when running locally with dummy test key
        if (apiKey == null || apiKey.startsWith("sk_test_mock") || "sk_test_123".equals(apiKey)) {
            log.info("Simulating Stripe charge in local/mock mode: token={}, amount={}, currency={}", token, amount, currency);
            if ("tok_chargeDeclined".equals(token) || "tok_chargeCustomerFail".equals(token)) {
                throw new RuntimeException("Card declined (simulated Stripe decline for token: " + token + ")");
            }
            Charge mockCharge = new Charge();
            mockCharge.setId("ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
            return mockCharge;
        }

        // Real Stripe API call
        long amountInCents = amount.multiply(new BigDecimal(100)).longValue();

        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", amountInCents);
        chargeParams.put("currency", currency);
        chargeParams.put("description", description);
        chargeParams.put("source", token);

        return Charge.create(chargeParams);
    }
}
