package com.cms.client.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TicketResponse {
    private Long ticketId;
    private Long clientId;
    private Long accountId;
    private String subject;
    private String description;
    private String status;
    private String priority;
    private String category;
    private Long assignedTo;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
    private Instant closedAt;
    private List<TicketCommentResponse> comments;
}
