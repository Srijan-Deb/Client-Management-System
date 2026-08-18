-- =============================================================================
-- V4 - contacts table
-- Each client can have multiple contacts (PRIMARY, SECONDARY, BILLING).
-- Cascade-deleted when the parent client is deleted.
-- =============================================================================

CREATE TABLE contacts
(
    contact_id   BIGINT                                         NOT NULL AUTO_INCREMENT,
    client_id    BIGINT                                         NOT NULL,
    contact_type ENUM ('PRIMARY', 'SECONDARY', 'BILLING')      NOT NULL DEFAULT 'PRIMARY',
    first_name   VARCHAR(100)                                   NOT NULL,
    last_name    VARCHAR(100)                                   NOT NULL,
    email        VARCHAR(255)                                            DEFAULT NULL,
    phone        VARCHAR(20)                                             DEFAULT NULL,
    created_at   DATETIME(6)                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (contact_id),
    KEY idx_contacts_client_id (client_id),
    CONSTRAINT fk_contacts_client
        FOREIGN KEY (client_id) REFERENCES clients (client_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
