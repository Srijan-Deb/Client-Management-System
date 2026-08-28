package com.cms.client.controller;

import com.cms.client.repository.ClientRepository;
import com.cms.client.repository.SupportTicketRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class ClientDashboardController {

    private final ClientRepository clientRepository;
    private final SupportTicketRepository supportTicketRepository;

    public ClientDashboardController(ClientRepository clientRepository, SupportTicketRepository supportTicketRepository) {
        this.clientRepository = clientRepository;
        this.supportTicketRepository = supportTicketRepository;
    }

    @GetMapping("/metrics")
    public Map<String, Long> getMetrics() {
        long totalClients = clientRepository.count();
        long openTickets = supportTicketRepository.countByStatus("OPEN");
        
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("totalClients", totalClients);
        metrics.put("openTickets", openTickets);
        return metrics;
    }
}
