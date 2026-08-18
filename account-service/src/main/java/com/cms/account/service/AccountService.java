package com.cms.account.service;

import com.cms.account.dto.AccountResponse;
import com.cms.account.dto.LinkAccountRequest;

/**
 * Business operations for the Account Service.
 */
public interface AccountService {

    /**
     * Creates a new account linked to the given client (by storing {@code account_id}
     * back on the client row â€” done by the caller, not here).
     *
     * <p>Caches the resulting {@link AccountResponse} under {@code account:{accountId}}.
     *
     * @param clientId path variable forwarded from Client Service for logging/correlation only
     * @param request  validated body containing name and email
     * @return populated {@link AccountResponse} containing the new {@code accountId}
     */
    AccountResponse linkAccount(Long clientId, LinkAccountRequest request);

    /**
     * Returns account by ID. Redis cache-aside: hit â†’ return cached; miss â†’ DB â†’ cache.
     *
     * @param accountId primary key
     * @return {@link AccountResponse}
     * @throws com.cms.common.exception.ResourceNotFoundException when not found
     */
    AccountResponse getAccountById(Long accountId);
}
