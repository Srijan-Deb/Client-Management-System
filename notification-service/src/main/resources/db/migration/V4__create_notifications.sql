-- =============================================================================
-- V4 - notifications
-- Audit trail of every email sent (or failed). Written after each send attempt
-- so we have a durable record of what was sent and to whom - replaces ELK for
-- notification-specific audit (per v2's decision to drop ELK from the stack).
-- =============================================================================

CREATE TABLE notifications
(
    notification_id BIGINT                                 NOT NULL AUTO_INCREMENT,
    event_type      VARCHAR(50)                            NOT NULL,
    recipient_email VARCHAR(255)                           NOT NULL,
    subject         VARCHAR(255)                           NOT NULL,
    status          ENUM ('SENT', 'FAILED', 'PENDING')     NOT NULL DEFAULT 'PENDING',
    error_message   TEXT                                   NULL     COMMENT 'Populated on FAILED',
    sent_at         DATETIME(6)                            NULL     COMMENT 'Populated on SENT',
    created_at      DATETIME(6)                            NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (notification_id),
    KEY idx_notifications_event_type      (event_type),
    KEY idx_notifications_recipient_email (recipient_email),
    KEY idx_notifications_status          (status),
    KEY idx_notifications_sent_at         (sent_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Durable audit trail of all email notifications - sent, failed, and pending';
