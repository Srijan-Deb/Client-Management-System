-- =============================================================================
-- V6 - accounts table
-- Stores account records. An account can own multiple clients (1:many).
-- The FK lives on clients.account_id - accounts has no reference back to
-- any individual client, which is the correct direction for the schema.
-- Created synchronously during client onboarding via /api/v1/accounts/link/{clientId}.
-- =============================================================================

CREATE TABLE accounts
(
    account_id   BIGINT                                        NOT NULL AUTO_INCREMENT,
    account_name VARCHAR(255)                                  NOT NULL,
    email        VARCHAR(255)                                  NOT NULL,
    status       ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED')     NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME(6)                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (account_id),
    KEY idx_accounts_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
