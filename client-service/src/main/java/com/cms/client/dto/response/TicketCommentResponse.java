package com.cms.client.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TicketCommentResponse {
    private Long commentId;
    private Long ticketId;
    private Long authorId;
    private String commentText;
    private Instant createdAt;
}
