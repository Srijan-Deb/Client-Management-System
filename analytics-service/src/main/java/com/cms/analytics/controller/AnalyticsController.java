package com.cms.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/client-growth")
    public List<Map<String, Object>> getClientGrowth() {
        // Group by month for the last 6 months
        String sql = """
            SELECT 
                formatDateTime(created_at, '%b %y') AS label,
                count(*) AS clients
            FROM clients_analytics
            WHERE created_at >= subtractMonths(now(), 6)
            GROUP BY toStartOfMonth(created_at), label
            ORDER BY toStartOfMonth(created_at) ASC
        """;
        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/ticket-status")
    public List<Map<String, Object>> getTicketStatus() {
        // ReplacingMergeTree requires us to select the final state, so we use argMax or optimize.
        // For simplicity in this demo, since we are doing simple counts, we can just group by status
        // and take the latest row per ticket_id.
        String sql = """
            SELECT 
                status AS name,
                count(*) AS value
            FROM (
                SELECT ticket_id, argMax(status, updated_at) AS status
                FROM tickets_analytics
                GROUP BY ticket_id
            )
            GROUP BY status
        """;
        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/ticket-priority")
    public List<Map<String, Object>> getTicketPriority() {
        String sql = """
            SELECT 
                priority AS label,
                count(*) AS value
            FROM (
                SELECT ticket_id, argMax(priority, updated_at) AS priority
                FROM tickets_analytics
                GROUP BY ticket_id
            )
            GROUP BY priority
        """;
        return jdbcTemplate.queryForList(sql);
    }

    @GetMapping("/invoice-status")
    public List<Map<String, Object>> getInvoiceStatus() {
        String sql = """
            SELECT 
                status AS name,
                count(*) AS value
            FROM (
                SELECT invoice_id, argMax(status, updated_at) AS status
                FROM invoices_analytics
                GROUP BY invoice_id
            )
            GROUP BY status
        """;
        return jdbcTemplate.queryForList(sql);
    }
}
