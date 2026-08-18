package com.cms.client.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketCommentRequest {
    @NotBlank(message = "commentText is required")
    private String commentText;
}
