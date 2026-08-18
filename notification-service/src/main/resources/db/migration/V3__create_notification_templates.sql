-- =============================================================================
-- V3 - notification_templates
-- Seeded in V5. One row per event type - templates are data-driven so adding
-- a new event type never requires a code deploy (just a new seed row + template file).
-- =============================================================================

CREATE TABLE notification_templates
(
    template_id        BIGINT       NOT NULL AUTO_INCREMENT,
    event_type         VARCHAR(50)  NOT NULL COMMENT 'Matches Kafka event type enum: CLIENT_ONBOARDED, INVOICE_GENERATED, â€¦',
    subject            VARCHAR(255) NOT NULL COMMENT 'Email subject line',
    body_template_name VARCHAR(100) NOT NULL COMMENT 'Thymeleaf template filename stem (no path, no .html)',
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (template_id),
    UNIQUE KEY uq_templates_event_type (event_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'One template row per Kafka event type - data-driven, no redeploy needed to add a template';
