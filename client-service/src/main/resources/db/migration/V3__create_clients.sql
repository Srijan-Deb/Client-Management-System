-- =============================================================================
-- V3 - clients table
-- account_id is nullable at the DB level: Client Service INSERTs with NULL,
-- then synchronously calls Account Service (inside the same @Transactional
-- boundary) to get account_id, then UPDATEs before commit.
-- If Account Service fails -> Spring rolls back -> nothing persisted.
-- =============================================================================

CREATE TABLE clients
(
    client_id    BIGINT                                         NOT NULL AUTO_INCREMENT,
    account_id   BIGINT                                                  DEFAULT NULL,
    first_name   VARCHAR(100)                                  NOT NULL,
    last_name    VARCHAR(100)                                  NOT NULL,
    email        VARCHAR(255)                                  NOT NULL,
    phone        VARCHAR(20)                                             DEFAULT NULL,
    company_name VARCHAR(255)                                            DEFAULT NULL,
    tier         ENUM ('STANDARD', 'PREMIUM', 'ENTERPRISE')   NOT NULL  DEFAULT 'STANDARD',
    status       ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED')     NOT NULL  DEFAULT 'ACTIVE',
    created_by   BIGINT                                                  DEFAULT NULL,
    created_at   DATETIME(6)                                  NOT NULL  DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)                                  NOT NULL  DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (client_id),
    UNIQUE KEY uq_clients_email (email),
    KEY idx_clients_status (status),
    KEY idx_clients_tier (tier),
    KEY idx_clients_account_id (account_id),
    CONSTRAINT fk_clients_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
        ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
