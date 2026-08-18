package com.cms.billing.service;

import com.cms.billing.domain.dto.SubscriptionRequest;
import com.cms.billing.domain.entity.Product;
import com.cms.billing.domain.entity.Subscription;
import com.cms.billing.repository.ProductRepository;
import com.cms.billing.repository.SubscriptionRepository;
import com.cms.common.event.SubscriptionCreatedEvent;
import java.time.LocalDate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               ProductRepository productRepository,
                               KafkaTemplate<String, Object> kafkaTemplate) {
        this.subscriptionRepository = subscriptionRepository;
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public Subscription createSubscription(SubscriptionRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Subscription subscription = new Subscription();
        subscription.setClientId(request.getClientId());
        subscription.setProduct(product);
        subscription.setStatus("ACTIVE");
        subscription.setStartDate(LocalDate.now());

        // Calculate next billing date
        LocalDate nextBilling = LocalDate.now();
        if ("MONTHLY".equalsIgnoreCase(product.getBillingCycle())) {
            nextBilling = nextBilling.plusMonths(1);
        } else if ("YEARLY".equalsIgnoreCase(product.getBillingCycle())) {
            nextBilling = nextBilling.plusYears(1);
        }
        subscription.setNextBillingDate(nextBilling);

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // Publish event
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                savedSubscription.getId(),
                savedSubscription.getClientId(),
                savedSubscription.getProduct().getId(),
                request.getRecipientEmail()
        );
        kafkaTemplate.send("subscription-created", event);

        return savedSubscription;
    }
}
