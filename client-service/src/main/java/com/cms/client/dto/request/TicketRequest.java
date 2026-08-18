package com.cms.client.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketRequest {
    
    @NotNull(message = "clientId is required")
    private Long clientId;
    
    private Long accountId;

    @NotBlank(message = "subject is required")
    private String subject;

    @NotBlank(message = "description is required")
    private String description;

    @NotBlank(message = "priority is required")
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL

    @NotBlank(message = "category is required")
    private String category;
}
