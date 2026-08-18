-- =============================================================================
-- V5__seed_roles.sql  (cms_account schema)
-- Seeds the three CMS application roles. role_name values match exactly
-- what was created in Keycloak via keycloak-provision.sh.
-- =============================================================================

INSERT INTO roles (role_name, description) VALUES
    ('admin',
     'Full system access â€” user management, configuration, all data visibility'),
    ('account_manager',
     'Manages client accounts, contracts, invoices; cannot access system config'),
    ('support_agent',
     'Handles support tickets and views client data; billing read-only access')
ON DUPLICATE KEY UPDATE description = VALUES(description);
-- ON DUPLICATE KEY UPDATE makes this re-runnable if V5 is ever re-applied
