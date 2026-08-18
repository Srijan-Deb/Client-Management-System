-- =============================================================================
-- V2__create_users.sql  (cms_account schema â€” AUTHORITATIVE)
-- Full user record including department (not in projection tables).
-- Populated by account-service UserSyncFilter on first authenticated hit.
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    user_id      BIGINT        NOT NULL AUTO_INCREMENT,
    keycloak_id  VARCHAR(36)   NOT NULL COMMENT 'Keycloak sub (UUID) â€” unique sync key',
    full_name    VARCHAR(255)  NOT NULL,
    email        VARCHAR(255)  NOT NULL,
    department   VARCHAR(100)           DEFAULT NULL,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    last_login   DATETIME(6)            DEFAULT NULL,
    created_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_keycloak_id (keycloak_id),
    UNIQUE KEY uq_users_email       (email),
    KEY        idx_users_department (department),
    KEY        idx_users_is_active  (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Authoritative user table â€” synced from Keycloak JWT on login';
