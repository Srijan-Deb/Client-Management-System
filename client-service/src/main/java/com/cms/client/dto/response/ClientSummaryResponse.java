package com.cms.client.dto.response;

import com.cms.client.domain.enums.ClientStatus;
import com.cms.client.domain.enums.ClientTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lightweight client summary â€” returned by the paginated GET /clients?search= endpoint.
 * Omits contacts and addresses to reduce payload size on list responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSummaryResponse {

    private Long clientId;
    private Long accountId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String companyName;
    private ClientTier tier;
    private ClientStatus status;
    private Instant createdAt;
}
