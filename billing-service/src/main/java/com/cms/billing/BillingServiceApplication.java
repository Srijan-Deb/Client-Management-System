package com.cms.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CMS Billing Service â€” entry point.
 *
 * <p><b>Responsibilities (by build phase):</b>
 * <ul>
 *   <li>Phase 5: Product catalogue, subscriptions, contracts, invoice generation,
 *       PDF creation via OpenPDF, MinIO upload, payment processing with retry logic</li>
 *   <li>Publishes {@code InvoiceGeneratedEvent}, {@code PaymentSuccessEvent},
 *       {@code PaymentFailedEvent}</li>
 * </ul>
 *
 * <p>{@code @EnableScheduling} is added now for future scheduled invoice
 * generation jobs (Phase 5).
 *
 * <p><b>Port:</b> 8083 (see application.yml)
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableScheduling
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
