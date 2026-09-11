package com.cms.account.repository;

import com.cms.account.domain.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /** Used by Phase 3 expansion to detect existing accounts by email. */
    Optional<Account> findByEmail(String email);

    /** Paginated list with optional search on accountName or email (dashboard). */
    @Query("SELECT a FROM Account a WHERE " +
           "(:search IS NULL OR LOWER(a.accountName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Account> findAllBySearch(@Param("search") String search, Pageable pageable);
}

