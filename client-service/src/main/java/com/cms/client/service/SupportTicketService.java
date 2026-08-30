package com.cms.client.service;

import com.cms.client.domain.entity.*;
import com.cms.client.dto.request.TicketCommentRequest;
import com.cms.client.dto.request.TicketRequest;
import com.cms.client.dto.response.TicketCommentResponse;
import com.cms.client.dto.response.TicketResponse;
import com.cms.client.repository.ActivityLogRepository;
import com.cms.client.repository.ClientRepository;
import com.cms.client.repository.SupportTicketRepository;
import com.cms.client.repository.UserProjectionRepository;
import com.cms.common.event.TicketCreatedEvent;
import com.cms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final ClientRepository clientRepository;
    private final UserProjectionRepository userProjectionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public TicketResponse createTicket(TicketRequest request, Jwt jwt) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND", "Client not found"));

        UserProjection user = userProjectionRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        SupportTicket ticket = SupportTicket.builder()
                .clientId(request.getClientId())
                .accountId(request.getAccountId())
                .subject(request.getSubject())
                .description(request.getDescription())
                .status("OPEN")
                .priority(request.getPriority())
                .category(request.getCategory())
                .build();

        SupportTicket savedTicket = ticketRepository.save(ticket);

        ActivityLog logEntry = ActivityLog.builder()
                .clientId(client.getClientId())
                .userId(user.getUserId())
                .action("TICKET_CREATED")
                .entityType("TICKET")
                .entityId(savedTicket.getTicketId())
                .description("Created support ticket: " + request.getSubject())
                .build();
        activityLogRepository.save(logEntry);

        // Fetch client email from primary contact, or default
        String recipientEmail = client.getContacts().stream()
                .filter(c -> c.getContactType() == com.cms.client.domain.enums.ContactType.PRIMARY)
                .map(Contact::getEmail)
                .findFirst()
                .orElse(client.getContacts().isEmpty() ? "fallback@example.com" : client.getContacts().get(0).getEmail());

        TicketCreatedEvent event = TicketCreatedEvent.builder()
                .ticketId(savedTicket.getTicketId())
                .clientId(savedTicket.getClientId())
                .recipientEmail(recipientEmail)
                .accountId(savedTicket.getAccountId())
                .subject(savedTicket.getSubject())
                .description(savedTicket.getDescription())
                .priority(savedTicket.getPriority())
                .category(savedTicket.getCategory())
                .createdAt(savedTicket.getCreatedAt())
                .build();
                
        kafkaTemplate.send(TicketCreatedEvent.TOPIC, event);

        return mapToResponse(savedTicket);
    }

    @Transactional
    public TicketResponse assignTicket(Long ticketId, Long agentId, Jwt jwt) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("TICKET_NOT_FOUND", "Ticket not found"));

        UserProjection user = userProjectionRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        ticket.setAssignedTo(agentId);
        ticket.setStatus("IN_PROGRESS");
        SupportTicket savedTicket = ticketRepository.save(ticket);

        ActivityLog logEntry = ActivityLog.builder()
                .clientId(ticket.getClientId())
                .userId(user.getUserId())
                .action("TICKET_ASSIGNED")
                .entityType("TICKET")
                .entityId(savedTicket.getTicketId())
                .description("Assigned ticket to agent ID: " + agentId)
                .build();
        activityLogRepository.save(logEntry);

        return mapToResponse(savedTicket);
    }

    @Transactional
    public TicketResponse resolveTicket(Long ticketId, Jwt jwt) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("TICKET_NOT_FOUND", "Ticket not found"));

        UserProjection user = userProjectionRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        ticket.setStatus("RESOLVED");
        SupportTicket savedTicket = ticketRepository.save(ticket);

        ActivityLog logEntry = ActivityLog.builder()
                .clientId(ticket.getClientId())
                .userId(user.getUserId())
                .action("TICKET_RESOLVED")
                .entityType("TICKET")
                .entityId(savedTicket.getTicketId())
                .description("Resolved support ticket")
                .build();
        activityLogRepository.save(logEntry);

        return mapToResponse(savedTicket);
    }

    @Transactional
    public TicketResponse reopenTicket(Long ticketId, Jwt jwt) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("TICKET_NOT_FOUND", "Ticket not found"));

        UserProjection user = userProjectionRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        ticket.setStatus("OPEN");
        SupportTicket savedTicket = ticketRepository.save(ticket);

        ActivityLog logEntry = ActivityLog.builder()
                .clientId(ticket.getClientId())
                .userId(user.getUserId())
                .action("TICKET_REOPENED")
                .entityType("TICKET")
                .entityId(savedTicket.getTicketId())
                .description("Reopened support ticket")
                .build();
        activityLogRepository.save(logEntry);

        return mapToResponse(savedTicket);
    }

    @Transactional
    public TicketResponse addComment(Long ticketId, TicketCommentRequest request, Jwt jwt) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("TICKET_NOT_FOUND", "Ticket not found"));

        UserProjection user = userProjectionRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .authorId(user.getUserId())
                .commentText(request.getCommentText())
                .build();

        ticket.addComment(comment);
        SupportTicket savedTicket = ticketRepository.save(ticket);

        ActivityLog logEntry = ActivityLog.builder()
                .clientId(ticket.getClientId())
                .userId(user.getUserId())
                .action("TICKET_COMMENT_ADDED")
                .entityType("TICKET_COMMENT")
                .entityId(comment.getCommentId())
                .description("Added comment to ticket")
                .build();
        activityLogRepository.save(logEntry);

        return mapToResponse(savedTicket);
    }

    @Transactional
    public TicketResponse closeTicket(Long ticketId, Jwt jwt) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("TICKET_NOT_FOUND", "Ticket not found"));

        UserProjection user = userProjectionRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        ticket.setStatus("CLOSED");
        SupportTicket savedTicket = ticketRepository.save(ticket);

        ActivityLog logEntry = ActivityLog.builder()
                .clientId(ticket.getClientId())
                .userId(user.getUserId())
                .action("TICKET_CLOSED")
                .entityType("TICKET")
                .entityId(savedTicket.getTicketId())
                .description("Closed support ticket")
                .build();
        activityLogRepository.save(logEntry);

        return mapToResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("TICKET_NOT_FOUND", "Ticket not found: " + ticketId));
        return mapToResponse(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> getTicketsByClient(Long clientId, Pageable pageable) {
        if (clientId == null) {
            return ticketRepository.findAll(pageable).map(this::mapToResponse);
        }
        return ticketRepository.findByClientId(clientId, pageable)
                .map(this::mapToResponse);
    }

    private TicketResponse mapToResponse(SupportTicket ticket) {
        List<TicketCommentResponse> commentResponses = ticket.getComments().stream()
                .map(c -> TicketCommentResponse.builder()
                        .commentId(c.getCommentId())
                        .ticketId(ticket.getTicketId())
                        .authorId(c.getAuthorId())
                        .commentText(c.getCommentText())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return TicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .clientId(ticket.getClientId())
                .accountId(ticket.getAccountId())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .assignedTo(ticket.getAssignedTo())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .comments(commentResponses)
                .build();
    }
}
