package com.cms.client.domain.enums;

/**
 * Lifecycle status of a client record.
 * Stored as a MySQL ENUM('ACTIVE','INACTIVE','SUSPENDED').
 */
public enum ClientStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
