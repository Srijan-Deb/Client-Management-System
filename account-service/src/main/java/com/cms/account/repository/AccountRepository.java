package com.cms.account.repository;

import com.cms.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /** Used by Phase 3 expansion to detect existing accounts by email. */
    Optional<Account> findByEmail(String email);
}

