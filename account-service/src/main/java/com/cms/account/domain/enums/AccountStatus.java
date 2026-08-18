package com.cms.account.domain.enums;

/**
 * Lifecycle status of an account record.
 * Stored as MySQL ENUM('ACTIVE','INACTIVE','SUSPENDED').
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
