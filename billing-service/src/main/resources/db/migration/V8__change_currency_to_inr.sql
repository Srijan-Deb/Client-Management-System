-- =============================================================================
-- V8__change_currency_to_inr.sql (cms_billing schema)
-- Change default currency from USD to INR for products and invoices
-- =============================================================================

-- Change defaults for new records
ALTER TABLE products MODIFY currency VARCHAR(3) DEFAULT 'INR';
ALTER TABLE invoices MODIFY currency VARCHAR(10) DEFAULT 'INR';

-- Update existing records to INR if they were USD
UPDATE products SET currency = 'INR' WHERE currency = 'USD';
UPDATE invoices SET currency = 'INR' WHERE currency = 'USD';
