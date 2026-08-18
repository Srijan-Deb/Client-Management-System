-- =============================================================================
-- V6 - add subscription_created to notification_templates
-- =============================================================================

INSERT INTO notification_templates (event_type, subject, body_template_name) VALUES
    ('SUBSCRIPTION_CREATED', 'Subscription Confirmed - Welcome aboard!', 'subscription-created');
