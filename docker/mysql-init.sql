-- =============================================================
-- CMS MySQL Initialisation Script
-- Runs once on first container start (docker-entrypoint-initdb.d)
-- Creates all application schemas + grants to cms_user
-- =============================================================

-- Client Service schema
CREATE DATABASE IF NOT EXISTS cms_client
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Account Service schema
CREATE DATABASE IF NOT EXISTS cms_account
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Billing Service schema
CREATE DATABASE IF NOT EXISTS cms_billing
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Notification Service schema
CREATE DATABASE IF NOT EXISTS cms_notification
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Keycloak schema (isolated from application data per v2 architecture)
CREATE DATABASE IF NOT EXISTS keycloak_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Grant all privileges on application schemas to the app user
GRANT ALL PRIVILEGES ON cms_client.*       TO 'cms_user'@'%';
GRANT ALL PRIVILEGES ON cms_account.*      TO 'cms_user'@'%';
GRANT ALL PRIVILEGES ON cms_billing.*      TO 'cms_user'@'%';
GRANT ALL PRIVILEGES ON cms_notification.* TO 'cms_user'@'%';
GRANT ALL PRIVILEGES ON keycloak_db.*       TO 'cms_user'@'%';

FLUSH PRIVILEGES;
