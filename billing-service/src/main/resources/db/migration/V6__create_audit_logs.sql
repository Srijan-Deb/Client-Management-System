-- =============================================================================
-- V6__create_audit_logs.sql (cms_billing schema)
-- Create audit_logs table for mutating actions
-- =============================================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    entity_name  VARCHAR(100) NOT NULL,
    entity_id    BIGINT       NOT NULL,
    action       VARCHAR(100) NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    details      TEXT         DEFAULT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_audit_logs_entity (entity_name, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
