package com.cms.client.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    private Long logId;
    private Long clientId;
    private Long userId;
    private String action;
    private String entityType;
    private Long entityId;
    private String description;
    private String ipAddress;
    private Instant createdAt;
}
