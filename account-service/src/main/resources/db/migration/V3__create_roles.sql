-- =============================================================================
-- V3__create_roles.sql  (cms_account schema)
-- Role definitions â€” managed by admins, mirrored from Keycloak realm roles.
-- Other services read roles from JWT claims, not from this table directly.
-- =============================================================================

CREATE TABLE IF NOT EXISTS roles (
    role_id     BIGINT        NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50)   NOT NULL COMMENT 'Matches Keycloak realm role name exactly',
    description VARCHAR(255)           DEFAULT NULL,
    permissions JSON                   DEFAULT NULL COMMENT 'Fine-grained permission list (Phase 8)',

    PRIMARY KEY (role_id),
    UNIQUE KEY uq_roles_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Role definitions â€” mirrors Keycloak realm roles for local RBAC management';
