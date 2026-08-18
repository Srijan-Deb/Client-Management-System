-- =============================================================================
-- V5 â€” seed notification_templates
-- One row per Kafka event type. body_template_name maps to a Thymeleaf HTML
-- file in resources/templates/email/{name}.html
-- =============================================================================

INSERT INTO notification_templates (event_type, subject, body_template_name) VALUES
    ('CLIENT_ONBOARDED',    'Welcome to CMS â€” Your account is ready',          'client-onboarded'),
    ('INVOICE_GENERATED',   'Your invoice is ready â€” Please review and pay',     'invoice-generated'),
    ('PAYMENT_SUCCESS',     'Payment received â€” Thank you!',                     'payment-success'),
    ('PAYMENT_FAILED',      'Action required â€” Your payment could not be processed', 'payment-failed'),
    ('TICKET_CREATED',      'Support ticket received â€” We''re on it',            'ticket-created');
