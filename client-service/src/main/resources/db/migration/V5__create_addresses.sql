-- =============================================================================
-- V5 - addresses table
-- Each client can have multiple addresses (BILLING, SHIPPING, PRIMARY).
-- is_primary flags the canonical address for correspondence.
-- Cascade-deleted when the parent client is deleted.
-- =============================================================================

CREATE TABLE addresses
(
    address_id   BIGINT                                          NOT NULL AUTO_INCREMENT,
    client_id    BIGINT                                          NOT NULL,
    address_type ENUM ('BILLING', 'SHIPPING', 'PRIMARY')        NOT NULL DEFAULT 'PRIMARY',
    line1        VARCHAR(255)                                    NOT NULL,
    line2        VARCHAR(255)                                             DEFAULT NULL,
    city         VARCHAR(100)                                    NOT NULL,
    state        VARCHAR(100)                                             DEFAULT NULL,
    postal_code  VARCHAR(20)                                     NOT NULL,
    country      VARCHAR(100)                                    NOT NULL DEFAULT 'India',
    is_primary   BOOLEAN                                         NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (address_id),
    KEY idx_addresses_client_id (client_id),
    CONSTRAINT fk_addresses_client
        FOREIGN KEY (client_id) REFERENCES clients (client_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
