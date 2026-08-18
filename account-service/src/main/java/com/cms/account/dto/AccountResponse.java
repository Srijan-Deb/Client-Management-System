package com.cms.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;

/**
 * Response DTO for account endpoints.
 *
 * <p>Implements {@link Serializable} so it can be stored in Redis
 * (Jackson-serialized, but Serializable satisfies Spring Cache contracts).
 *
 * <p>{@code @JsonInclude(NON_NULL)} keeps the payload clean â€” null fields
 * (e.g. missing optional columns added in Phase 3) are omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(
        Long    accountId,
        String  accountName,
        String  email,
        String  status,
        Instant createdAt
) implements Serializable {
}
