-- =============================================================================
-- V5__create_contracts_and_invoices.sql (cms_billing schema)
-- Create contracts and invoices tables and alter subscriptions
-- =============================================================================

CREATE TABLE IF NOT EXISTS contracts (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    client_id     BIGINT        NOT NULL,
    account_id    BIGINT        NOT NULL,
    status        VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE',
    total_value   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    pdf_url       VARCHAR(500)  DEFAULT NULL,
    start_date    DATE          NOT NULL,
    end_date      DATE          DEFAULT NULL,
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_contracts_client_id (client_id),
    KEY idx_contracts_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoices (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    contract_id    BIGINT        DEFAULT NULL,
    client_id      BIGINT        NOT NULL,
    account_id     BIGINT        NOT NULL,
    invoice_number VARCHAR(100)  NOT NULL,
    subtotal       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax_rate       DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    tax_amount     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    currency       VARCHAR(10)   NOT NULL DEFAULT 'USD',
    due_date       DATE          NOT NULL,
    status         VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    pdf_object_key VARCHAR(500)  DEFAULT NULL,
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_invoices_number (invoice_number),
    KEY idx_invoices_client_id (client_id),
    KEY idx_invoices_account_id (account_id),
    CONSTRAINT fk_invoices_contract FOREIGN KEY (contract_id) REFERENCES contracts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE subscriptions
ADD COLUMN contract_id BIGINT DEFAULT NULL,
ADD CONSTRAINT fk_subscriptions_contract FOREIGN KEY (contract_id) REFERENCES contracts(id);
