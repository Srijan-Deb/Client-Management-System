package com.cms.account.controller;

import com.cms.account.dto.AccountResponse;
import com.cms.account.dto.LinkAccountRequest;
import com.cms.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


/**
 * REST controller for account management.
 *
 * <p><b>POST /api/v1/accounts/link/{clientId}</b> â€” internal endpoint called
 * synchronously by Client Service during client onboarding. Permit-all in
 * {@code SecurityConfig} (Docker network isolation). Phase 8 adds mTLS / API-key.
 *
 * <p><b>GET /api/v1/accounts/{id}</b> â€” JWT-protected. Read access granted to all
 * three operational roles: {@code admin}, {@code account_manager}, {@code support_agent}.
 * Support agents need account data to triage tickets (tickets reference account_id).
 * Mutating endpoints (Phase 3+ additions) are restricted to admin/account_manager.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    /**
     * Create and link an account for a newly onboarded client.
     *
     * <p>Called by Client Service inside its {@code @Transactional} boundary.
     * If this call fails, Client Service rolls back the client INSERT â€” no partial state.
     *
     * <p>Security: permit-all (internal Docker network). See {@code SecurityConfig}.
     *
     * @param clientId path variable â€” used for logging/correlation; not stored on Account
     * @param request  validated body with firstName, lastName, email
     * @return 201 Created with {@code Location: /api/v1/accounts/{accountId}}
     */
    @PostMapping("/link/{clientId}")
    public ResponseEntity<AccountResponse> linkAccount(
            @PathVariable Long clientId,
            @Valid @RequestBody LinkAccountRequest request) {

        log.info("POST /accounts/link/{} â€” email={}", clientId, request.getEmail());
        AccountResponse response = accountService.linkAccount(clientId, request);
        URI location = URI.create("/api/v1/accounts/" + response.accountId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Retrieve an account by its primary key.
     *
     * <p>Redis cache-aside: returns cached value if present; falls back to MySQL.
     *
     * @param id account primary key
     * @return 200 OK with account details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent')")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    /**
     * Paginated list of all accounts — used by the admin dashboard Accounts page.
     *
     * @param search optional search term (matched against name or email)
     * @param page   0-indexed page number (default 0)
     * @param size   page size (default 20)
     * @return paginated {@link AccountResponse} list
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'account_manager', 'support_agent')")
    public ResponseEntity<Page<AccountResponse>> listAccounts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(accountService.getAllAccounts(search, pageable));
    }
}
