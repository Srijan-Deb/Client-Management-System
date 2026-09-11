package com.cms.analytics.listener;

import com.cms.common.event.ClientOnboardedEvent;
import com.cms.common.event.InvoiceGeneratedEvent;
import com.cms.common.event.PaymentSuccessEvent;
import com.cms.common.event.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsKafkaListener {

    private final JdbcTemplate jdbcTemplate;

    @KafkaListener(topics = ClientOnboardedEvent.TOPIC)
    public void handleClientOnboarded(ClientOnboardedEvent event) {
        log.info("Analytics consuming ClientOnboardedEvent for client ID: {}", event.getClientId());
        
        String sql = """
            INSERT INTO clients_analytics (client_id, company_name, tier, created_at)
            VALUES (?, ?, ?, ?)
        """;
        
        // Use firstName + lastName as fallback for companyName since companyName isn't in the event
        String name = event.getFirstName() + " " + event.getLastName();
        
        jdbcTemplate.update(sql, 
            String.valueOf(event.getClientId()),
            name,
            event.getTier() != null ? event.getTier() : "STANDARD",
            Timestamp.from(event.getOnboardedAt())
        );
    }

    @KafkaListener(topics = TicketCreatedEvent.TOPIC)
    public void handleTicketCreated(TicketCreatedEvent event) {
        log.info("Analytics consuming TicketCreatedEvent for ticket ID: {}", event.getTicketId());
        
        String sql = """
            INSERT INTO tickets_analytics (ticket_id, client_id, subject, status, priority, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        jdbcTemplate.update(sql,
            String.valueOf(event.getTicketId()),
            String.valueOf(event.getClientId()),
            event.getSubject(),
            "OPEN", // Initial status
            event.getPriority(),
            Timestamp.from(event.getCreatedAt()),
            Timestamp.from(event.getCreatedAt())
        );
    }

    @KafkaListener(topics = InvoiceGeneratedEvent.TOPIC)
    public void handleInvoiceGenerated(InvoiceGeneratedEvent event) {
        log.info("Analytics consuming InvoiceGeneratedEvent for invoice ID: {}", event.getInvoiceId());
        
        String sql = """
            INSERT INTO invoices_analytics (invoice_id, client_id, amount, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        jdbcTemplate.update(sql,
            String.valueOf(event.getInvoiceId()),
            String.valueOf(event.getClientId()),
            event.getTotalAmount(),
            "PENDING", // Initial status
            Timestamp.from(event.getGeneratedAt()),
            Timestamp.from(event.getGeneratedAt())
        );
    }

    @KafkaListener(topics = PaymentSuccessEvent.TOPIC)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Analytics consuming PaymentSuccessEvent for invoice ID: {}", event.getInvoiceId());
        
        // In ClickHouse, ReplacingMergeTree deduplicates by ORDER BY key (invoice_id) and keeps the one with the largest updated_at.
        // We re-insert the row with the new status "PAID". 
        // We don't have the original amount from the PaymentSuccessEvent, but we can assume amountPaid is the total,
        // or for true CDC we would look up the invoice first. For this scalable demo, we just use amountPaid.
        
        String sql = """
            INSERT INTO invoices_analytics (invoice_id, client_id, amount, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        jdbcTemplate.update(sql,
            String.valueOf(event.getInvoiceId()),
            String.valueOf(event.getClientId()),
            event.getAmountPaid(),
            "PAID", // Updated status
            Timestamp.from(event.getPaidAt()), // Close enough for created_at in a ReplacingMergeTree for this demo
            Timestamp.from(Instant.now())      // New updated_at to trigger replacing
        );
    }
}
