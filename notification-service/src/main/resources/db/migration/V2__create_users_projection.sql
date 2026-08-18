-- =============================================================================
-- V2__create_users_projection.sql  (cms_notification schema)
-- Local projection of user identity for FK integrity on:
--   notifications.user_id
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    user_id      BIGINT        NOT NULL AUTO_INCREMENT,
    keycloak_id  VARCHAR(36)   NOT NULL COMMENT 'Keycloak sub (UUID) â€” used as idempotent sync key',
    full_name    VARCHAR(255)  NOT NULL,
    email        VARCHAR(255)  NOT NULL,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    last_login   DATETIME(6)            DEFAULT NULL,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_keycloak_id (keycloak_id),
    UNIQUE KEY uq_users_email       (email),
    KEY        idx_users_is_active  (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Local user projection â€” synced from JWT on each authenticated request';
