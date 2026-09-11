package com.cms.account.service;

import com.cms.account.dto.AccountResponse;
import com.cms.account.dto.LinkAccountRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Business operations for the Account Service.
 */
public interface AccountService {

    /**
     * Creates a new account linked to the given client (by storing {@code account_id}
     * back on the client row — done by the caller, not here).
     *
     * <p>Caches the resulting {@link AccountResponse} under {@code account:{accountId}}.
     *
     * @param clientId path variable forwarded from Client Service for logging/correlation only
     * @param request  validated body containing name and email
     * @return populated {@link AccountResponse} containing the new {@code accountId}
     */
    AccountResponse linkAccount(Long clientId, LinkAccountRequest request);

    /**
     * Returns account by ID. Redis cache-aside: hit → return cached; miss → DB → cache.
     *
     * @param accountId primary key
     * @return {@link AccountResponse}
     * @throws com.cms.common.exception.ResourceNotFoundException when not found
     */
    AccountResponse getAccountById(Long accountId);

    /**
     * Returns a paginated, optionally-searched list of all accounts for the admin dashboard.
     *
     * @param search   optional search term matched against accountName or email (nullable)
     * @param pageable Spring Pageable (page, size, sort)
     * @return page of {@link AccountResponse}
     */
    Page<AccountResponse> getAllAccounts(String search, Pageable pageable);
}
