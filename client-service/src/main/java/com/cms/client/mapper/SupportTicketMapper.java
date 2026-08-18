package com.cms.client.mapper;

import com.cms.client.domain.entity.SupportTicket;
import com.cms.client.dto.request.TicketRequest;
import com.cms.client.dto.response.TicketResponse;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketMapper {

    public SupportTicket toEntity(TicketRequest request) {
        if (request == null) {
            return null;
        }

        return SupportTicket.builder()
                .clientId(request.getClientId())
                .accountId(request.getAccountId())
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(request.getPriority())
                .build();
    }

    public TicketResponse toResponse(SupportTicket ticket) {
        if (ticket == null) {
            return null;
        }

        return TicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .clientId(ticket.getClientId())
                .accountId(ticket.getAccountId())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .assignedTo(ticket.getAssignedTo())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
