package com.cms.account.service;

import com.cms.account.domain.entity.Account;
import com.cms.account.domain.enums.AccountStatus;
import com.cms.account.dto.AccountResponse;
import com.cms.account.dto.LinkAccountRequest;
import com.cms.account.repository.AccountRepository;
import com.cms.account.repository.ActivityLogRepository;
import com.cms.account.domain.entity.ActivityLog;
import com.cms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

/**
 * Implementation of {@link AccountService}.
 *
 * <p><b>Caching strategy:</b> Write-through on {@link #linkAccount} (cache immediately
 * after INSERT) and cache-aside on {@link #getAccountById} (populate on first miss).
 * Cache key: {@code account:{accountId}}, TTL 30 minutes.
 *
 * <p>Redis operations are intentionally not inside the transaction â€” we only
 * populate the cache after a successful DB commit to avoid caching uncommitted data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    static final String CACHE_PREFIX = "account:";
    static final Duration CACHE_TTL  = Duration.ofMinutes(30);

    private final AccountRepository                  accountRepository;
    private final RedisTemplate<String, Object>      redisTemplate;
    private final ActivityLogRepository              activityLogRepository;

    // â”€â”€ linkAccount â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Creates and persists a new account for the given client, then caches the result.
     *
     * <p><b>Idempotency:</b> If an account already exists for the given email (e.g. a
     * retry from Client Service after a timeout), the existing account is returned and
     * no duplicate is created.
     *
     * <p><b>Race-condition safety:</b> The V7 {@code UNIQUE} constraint on
     * {@code accounts.email} is the true guarantee. If two concurrent requests both
     * slip past the {@code findByEmail} check before either commits, the second INSERT
     * throws {@link DataIntegrityViolationException}. We catch that and fall back to
     * {@code findByEmail} so the caller sees the existing account (HTTP 200/201) rather
     * than an unhandled 500.
     *
     * <p>The {@code save()} call creates its own short transaction; {@code cacheAccount()}
     * is called after that transaction commits so the cache never holds uncommitted data.
     */
    @Override
    public AccountResponse linkAccount(Long clientId, LinkAccountRequest request) {
        log.info("Linking account for clientId={}, email={}", clientId, request.getEmail());

        // â”€â”€ Idempotency guard â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Optional<Account> existing = accountRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            log.warn("Account already exists for email={} (accountId={}) "
                             + "â€” returning existing, not creating duplicate",
                     request.getEmail(), existing.get().getAccountId());
            AccountResponse response = toResponse(existing.get());
            cacheAccount(response);
            return response;
        }

        // â”€â”€ Normal path â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Account account = Account.builder()
                .accountName(request.getFirstName() + " " + request.getLastName())
                .email(request.getEmail())
                .status(AccountStatus.ACTIVE)
                .build();

        try {
            // save() creates its own short transaction; cache populated after commit
            Account saved = accountRepository.save(account);
            log.info("Account created: accountId={} for clientId={}",
                     saved.getAccountId(), clientId);

            activityLogRepository.save(ActivityLog.builder()
                    .accountId(saved.getAccountId())
                    .action("ACCOUNT_CREATED")
                    .entityId(saved.getAccountId())
                    .description("Account created via client linking")
                    .build());

            AccountResponse response = toResponse(saved);
            cacheAccount(response); // safe: TX already committed at this point
            return response;

        } catch (DataIntegrityViolationException dive) {
            // Race-condition fallback: another thread inserted between our findByEmail
            // check and our INSERT. The V7 UNIQUE constraint on email caught it.
            log.warn("Concurrent INSERT collision for email={} (clientId={}) "
                             + "â€” falling back to existing account",
                     request.getEmail(), clientId);
            Account raceWinner = accountRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalStateException(
                            "DataIntegrityViolation but no account found for email: "
                            + request.getEmail()));
            AccountResponse response = toResponse(raceWinner);
            cacheAccount(response);
            return response;
        }
    }

    // â”€â”€ getAccountById â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId) {
        String key = CACHE_PREFIX + accountId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof AccountResponse hit) {
            log.debug("Cache HIT account:{}", accountId);
            return hit;
        }
        log.debug("Cache MISS account:{}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND", "Account not found with id: " + accountId));

        AccountResponse response = toResponse(account);
        cacheAccount(response);
        return response;
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getAccountName(),
                account.getEmail(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }

    private void cacheAccount(AccountResponse response) {
        String key = CACHE_PREFIX + response.accountId();
        redisTemplate.opsForValue().set(key, response, CACHE_TTL);
        log.debug("Cached account:{}", response.accountId());
    }
}
