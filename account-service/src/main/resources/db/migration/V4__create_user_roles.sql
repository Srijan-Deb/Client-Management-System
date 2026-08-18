-- =============================================================================
-- V4__create_user_roles.sql  (cms_account schema)
-- Junction table linking users to roles (mirrors Keycloak role assignments).
-- Unique constraint prevents duplicate assignments.
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_roles (
    user_role_id  BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    role_id       BIGINT        NOT NULL,
    assigned_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_role_id),
    UNIQUE KEY  uq_user_role          (user_id, role_id),
    CONSTRAINT  fk_user_roles_user    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT  fk_user_roles_role    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='User-role assignments â€” mirrors Keycloak realm role assignments';
