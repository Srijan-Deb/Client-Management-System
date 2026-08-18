-- =============================================================================
-- V6 - activity_logs table
-- Audit trail for all client-service operations.
-- Both FKs use ON DELETE SET NULL so logs survive entity deletion.
-- =============================================================================

CREATE TABLE activity_logs
(
    log_id      BIGINT        NOT NULL AUTO_INCREMENT,
    client_id   BIGINT                 DEFAULT NULL,
    user_id     BIGINT                 DEFAULT NULL,
    action      VARCHAR(100)  NOT NULL,
    entity_type VARCHAR(100)  NOT NULL DEFAULT 'CLIENT',
    entity_id   BIGINT                 DEFAULT NULL,
    description TEXT                   DEFAULT NULL,
    ip_address  VARCHAR(45)            DEFAULT NULL,
    created_at  DATETIME(6)  NOT NULL  DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (log_id),
    KEY idx_activity_client_id (client_id),
    KEY idx_activity_user_id (user_id),
    KEY idx_activity_created_at (created_at),
    CONSTRAINT fk_activity_client
        FOREIGN KEY (client_id) REFERENCES clients (client_id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
