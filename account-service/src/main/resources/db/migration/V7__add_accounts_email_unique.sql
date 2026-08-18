-- =============================================================================
-- V7 -- add unique constraint on accounts.email
-- =============================================================================
-- V6 created the table without a UNIQUE constraint on email. The application
-- layer performs a findByEmail check first, but two concurrent linkAccount
-- requests for the same email could both pass that check before either
-- commits. This constraint is the real database-level guarantee.
--
-- AccountServiceImpl catches DataIntegrityViolationException on the INSERT
-- and falls back to findByEmail, so a genuine race surfaces as a 200/existing
-- account rather than a 500.
--
-- NOTE: V6 already ran on existing environments, so this is a separate ALTER.
--       Do NOT edit V6 in place -- Flyway will reject the checksum mismatch.
-- =============================================================================

ALTER TABLE accounts
    ADD CONSTRAINT uq_accounts_email UNIQUE (email);
