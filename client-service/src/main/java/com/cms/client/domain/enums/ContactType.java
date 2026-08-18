package com.cms.client.domain.enums;

/**
 * Classification of a contact record attached to a client.
 * Stored as a MySQL ENUM('PRIMARY','SECONDARY','BILLING').
 */
public enum ContactType {
    PRIMARY,
    SECONDARY,
    BILLING
}
