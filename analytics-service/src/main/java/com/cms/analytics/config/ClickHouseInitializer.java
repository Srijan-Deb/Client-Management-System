package com.cms.analytics.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickHouseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        log.info("Initializing ClickHouse schemas...");
        int maxRetries = 10;
        int delay = 3000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                // Clients Analytics
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS clients_analytics (
                        client_id String,
                        company_name String,
                        tier String,
                        created_at DateTime
                    ) ENGINE = ReplacingMergeTree() 
                    ORDER BY (client_id)
                """);

                // Tickets Analytics
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS tickets_analytics (
                        ticket_id String,
                        client_id String,
                        subject String,
                        status String,
                        priority String,
                        created_at DateTime,
                        updated_at DateTime
                    ) ENGINE = ReplacingMergeTree(updated_at) 
                    ORDER BY (ticket_id)
                """);

                // Invoices Analytics
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS invoices_analytics (
                        invoice_id String,
                        client_id String,
                        amount Decimal(10, 2),
                        status String,
                        created_at DateTime,
                        updated_at DateTime
                    ) ENGINE = ReplacingMergeTree(updated_at) 
                    ORDER BY (invoice_id)
                """);

                log.info("ClickHouse schemas initialized successfully.");
                return;
            } catch (Exception e) {
                log.warn("Failed to initialize ClickHouse schema, attempt {} of {}. Retrying in {} ms. Cause: ", i + 1, maxRetries, delay, e);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Initialization interrupted", ie);
                }
            }
        }
        log.error("Exhausted retries. Could not initialize ClickHouse schemas.");
        throw new RuntimeException("Could not initialize ClickHouse schemas");
    }
}
