package com.cms.client.domain.enums;

/**
 * Client subscription/service tier.
 * Stored as a MySQL ENUM('STANDARD','PREMIUM','ENTERPRISE') via @Enumerated(EnumType.STRING).
 */
public enum ClientTier {
    STANDARD,
    PREMIUM,
    ENTERPRISE
}
