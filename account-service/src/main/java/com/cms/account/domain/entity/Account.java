package com.cms.account.domain.entity;

import com.cms.account.domain.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * JPA entity for the {@code accounts} table.
 *
 * <p>An account is the top-level owner: one account can be linked to many clients
 * (accounts 1 â†’ clients many). The FK {@code clients.account_id} is the owning side â€”
 * this entity has no {@code clientId} column and never will.
 *
 * <p>Created synchronously during client onboarding via
 * {@code POST /api/v1/accounts/link/{clientId}}. Full business logic
 * (credit limits, account types, billing cycles) is added in Phase 3.
 */
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "ENUM('ACTIVE','INACTIVE','SUSPENDED')")
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
